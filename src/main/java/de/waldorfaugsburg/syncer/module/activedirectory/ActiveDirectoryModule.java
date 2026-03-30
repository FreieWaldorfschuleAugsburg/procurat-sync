package de.waldorfaugsburg.syncer.module.activedirectory;

import de.waldorfaugsburg.syncer.SyncerApplication;
import de.waldorfaugsburg.syncer.module.AbstractModule;
import de.waldorfaugsburg.syncer.module.activedirectory.model.ActiveDirectoryAttribute;
import de.waldorfaugsburg.syncer.module.activedirectory.model.ActiveDirectoryUser;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.*;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;
import java.math.BigInteger;
import java.text.Normalizer;
import java.util.*;

@Slf4j
public class ActiveDirectoryModule extends AbstractModule {

    @Getter
    private ActiveDirectoryConfig config;
    private LdapContext ldapContext;

    public ActiveDirectoryModule(final SyncerApplication application) {
        super(application);
    }

    @Override
    public void init() throws Exception {
        config = getApplication().loadConfiguration("ad.json", ActiveDirectoryConfig.class);

        final Hashtable<String, String> env = new Hashtable<>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.SECURITY_AUTHENTICATION, "simple");
        env.put(Context.SECURITY_PRINCIPAL, config.getPrincipal());
        env.put(Context.SECURITY_CREDENTIALS, config.getPassword());
        env.put(Context.PROVIDER_URL, config.getHost());
        env.put(Context.SECURITY_PROTOCOL, "SSL");
        env.put("java.naming.ldap.factory.socket", UnsecuredSSLSocketFactory.class.getName());

        ldapContext = new InitialLdapContext(env, null);
    }

    @Override
    public void destroy() throws Exception {
        ldapContext.close();
    }

    public String generateUsername(final String firstName, final String lastName) throws NamingException {
        int i = 1;
        String username = null;

        while (username == null || findUserByUsername(username) != null) {
            username = config.getUsernamePrefix() + i + normalizeInput(firstName.substring(0, 2)) + normalizeInput(lastName.substring(0, 2));
            i++;
        }

        return username;
    }

    public String normalizeInput(final String input) {
        return Normalizer.normalize(input, Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", "").replaceAll(" ", "-").toLowerCase();
    }

    public ActiveDirectoryUPNStrategy getUpnStrategy(final String strategyName) {
        return config.getUpnStrategies().get(strategyName);
    }

    public void addGroupMember(final String groupDN, final String userDN) throws NamingException {
        final ModificationItem item = new ModificationItem(DirContext.ADD_ATTRIBUTE, new BasicAttribute("member", userDN));

        log.info("Add group member (groupDN: {}, userDN: {})", groupDN, userDN);
        ldapContext.modifyAttributes(groupDN, new ModificationItem[]{item});
    }

    public void removeGroupMember(final String groupDN, final String userDN) throws NamingException {
        final ModificationItem item = new ModificationItem(DirContext.REMOVE_ATTRIBUTE, new BasicAttribute("member", userDN));

        log.info("Remove group member (groupDN: {}, userDN: {})", groupDN, userDN);
        ldapContext.modifyAttributes(groupDN, new ModificationItem[]{item});
    }

    public Set<String> getGroupMembers(final String groupDN) throws NamingException {
        final Set<String> users = new HashSet<>();

        final SearchControls searchControls = new SearchControls();
        searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
        searchControls.setReturningAttributes(new String[]{"member"});

        final NamingEnumeration<SearchResult> results = ldapContext.search(groupDN, "(objectClass=*)", searchControls);
        if (results.hasMoreElements()) {
            final SearchResult result = results.next();
            final Attribute attribute = result.getAttributes().get("member");
            if (attribute != null) {
                final NamingEnumeration<?> members = attribute.getAll();

                while (members.hasMore()) {
                    final String memberDN = (String) members.next();
                    users.add(memberDN);
                }
            }
        }

        return users;
    }

    public void createUser(final String baseDN, final ActiveDirectoryUser user) throws NamingException {
        final String dn = "cn=" + user.getAttribute(ActiveDirectoryAttribute.CN) + "," + baseDN;
        user.setAttribute(ActiveDirectoryAttribute.DN, dn);

        log.info("blob");
        ldapContext.createSubcontext(dn, user.attributes());
        log.info("Created user (dn: {}, sAMAccountName: {})", user.getAttribute(ActiveDirectoryAttribute.DN),
                user.getAttribute(ActiveDirectoryAttribute.SAM_ACCOUNT_NAME));
    }

    public void updateUser(final ActiveDirectoryUser updatedUser) throws NamingException {
        final ActiveDirectoryUser currentUser = findUserByUsername(updatedUser.getAttribute(ActiveDirectoryAttribute.SAM_ACCOUNT_NAME));

        final List<ModificationItem> modifications = new ArrayList<>();
        for (final ActiveDirectoryAttribute attribute : ActiveDirectoryAttribute.values()) {
            final Object currentValue = currentUser.getAttribute(attribute);
            final Object updatedValue = updatedUser.getAttribute(attribute);

            if (updatedValue == null || (currentValue != null && currentValue.equals(updatedValue))) {
                continue;
            }

            if (attribute.isStaticAttribute()) {
                throw new IllegalStateException("static attribute " + attribute.name() + " cannot be changed");
            }

            modifications.add(new ModificationItem(DirContext.REPLACE_ATTRIBUTE, new BasicAttribute(attribute.getLdapAttributeName(), updatedValue)));
        }

        if (modifications.isEmpty()) {
            return;
        }

        log.info("blob");
        ldapContext.modifyAttributes((String) currentUser.getAttribute(ActiveDirectoryAttribute.DN), modifications.toArray(new ModificationItem[0]));
        log.info("Updated user (dn: {}, sAMAccountName: {}, modifications: {})", updatedUser.getAttribute(ActiveDirectoryAttribute.DN),
                updatedUser.getAttribute(ActiveDirectoryAttribute.SAM_ACCOUNT_NAME), modifications.size());
    }

    public ActiveDirectoryUser findUserByEmployeeId(final int employeeId) throws NamingException {
        final String searchFilter = "(&(objectClass=person)(employeeID=" + employeeId + "))";
        return findUserByFilter(config.getUsersDN(), searchFilter);
    }

    public ActiveDirectoryUser findUserByUsername(final String username) throws NamingException {
        final String searchFilter = "(&(objectClass=person)(sAMAccountName=" + username + "))";
        return findUserByFilter(config.getUsersDN(), searchFilter);
    }

    public ActiveDirectoryUser findUserByDn(final String dn) throws NamingException {
        final String searchFilter = "(&(objectClass=person)(distinguishedName=" + dn + "))";
        return findUserByFilter(config.getUsersDN(), searchFilter);
    }

    public ActiveDirectoryUser findUserByFilter(final String baseDN, final String filter) throws NamingException {
        final SearchControls searchControls = new SearchControls();
        searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);

        final NamingEnumeration<SearchResult> results = ldapContext.search(baseDN, filter, searchControls);
        if (!results.hasMoreElements()) {
            return null;
        }

        final SearchResult result = results.nextElement();
        if (results.hasMoreElements()) {
            throw new IllegalStateException("Found more than one user for filter '" + filter + "'");
        }

        return new ActiveDirectoryUser(result.getAttributes());
    }

    public List<ActiveDirectoryUser> findAllUsers() throws NamingException {
        return findAllUsers(config.getUsersDN());
    }

    public List<ActiveDirectoryUser> findAllUsers(final String baseDN) throws NamingException {
        final String searchFilter = "(objectClass=person)";
        final SearchControls searchControls = new SearchControls();
        searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);

        final NamingEnumeration<SearchResult> results = ldapContext.search(baseDN, searchFilter, searchControls);
        if (!results.hasMoreElements()) {
            return Collections.emptyList();
        }

        final List<ActiveDirectoryUser> users = new ArrayList<>();
        while (results.hasMoreElements()) {
            final SearchResult result = results.nextElement();
            users.add(new ActiveDirectoryUser(result.getAttributes()));
        }

        return users;
    }
}
