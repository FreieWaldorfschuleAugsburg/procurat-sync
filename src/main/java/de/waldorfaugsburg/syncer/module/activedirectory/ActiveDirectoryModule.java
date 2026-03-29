package de.waldorfaugsburg.syncer.module.activedirectory;

import de.waldorfaugsburg.syncer.SyncerApplication;
import de.waldorfaugsburg.syncer.module.AbstractModule;
import de.waldorfaugsburg.syncer.module.activedirectory.model.ActiveDirectoryAttribute;
import de.waldorfaugsburg.syncer.module.activedirectory.model.ActiveDirectoryUser;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.*;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;

public class ActiveDirectoryModule extends AbstractModule {

    private LdapContext ldapContext;

    public ActiveDirectoryModule(final SyncerApplication application) {
        super(application);
    }

    @Override
    public void init() throws Exception {
        final ActiveDirectoryConfig config = getApplication().loadConfiguration("ad.json", ActiveDirectoryConfig.class);

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

    }

    public void addToGroup(final ActiveDirectoryUser user, final String groupDN) throws NamingException {
        final ModificationItem item = new ModificationItem(DirContext.ADD_ATTRIBUTE, new BasicAttribute("member", user.getAttribute(ActiveDirectoryAttribute.DN)));
        ldapContext.modifyAttributes(groupDN, new ModificationItem[]{item});
    }

    public void isGroupMember(final ActiveDirectoryUser user, final String groupDN) throws NamingException {

    }

    public void createUser(final String baseDN, final ActiveDirectoryUser user) throws NamingException {
        final String fullName = user.getAttribute(ActiveDirectoryAttribute.GIVEN_NAME) + " " + user.getAttribute(ActiveDirectoryAttribute.SN);
        final String dn = "cn=" + fullName + "," + baseDN;

        user.setAttribute(ActiveDirectoryAttribute.DN, dn);
        ldapContext.createSubcontext(dn, user.attributes());
    }

    public void updateUser(final String baseDN, final ActiveDirectoryUser updatedUser) throws NamingException {
        final ActiveDirectoryUser currentUser = findUserByUsername(baseDN, updatedUser.getAttribute(ActiveDirectoryAttribute.SAM_ACCOUNT_NAME));

        final List<ModificationItem> modifications = new ArrayList<>();
        for (final ActiveDirectoryAttribute attribute : ActiveDirectoryAttribute.values()) {
            final Object currentValue = currentUser.getAttribute(attribute);
            final Object updatedValue = updatedUser.getAttribute(attribute);

            if (currentValue.equals(updatedValue)) {
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

        ldapContext.modifyAttributes((String) currentUser.getAttribute(ActiveDirectoryAttribute.DN), modifications.toArray(new ModificationItem[0]));
    }

    public ActiveDirectoryUser findUserByEmployeeId(final String baseDN, final int employeeId) throws NamingException {
        final String searchFilter = "(&(objectClass=person)(employeeID=" + employeeId + "))";
        return findUserByFilter(baseDN, searchFilter);
    }

    public ActiveDirectoryUser findUserByUsername(final String baseDN, final String username) throws NamingException {
        final String searchFilter = "(&(objectClass=person)(sAMAccountName=" + username + "))";
        return findUserByFilter(baseDN, searchFilter);
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
