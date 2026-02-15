package de.waldorfaugsburg.psync.client.activedirectory;

import com.cronutils.utils.Preconditions;
import de.waldorfaugsburg.psync.ProcuratSyncApplication;
import de.waldorfaugsburg.psync.client.AbstractClient;
import de.waldorfaugsburg.psync.client.activedirectory.model.ADUser;
import lombok.extern.slf4j.Slf4j;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.*;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j
public final class ADClient extends AbstractClient {

    private final ProcuratSyncApplication application;
    private LdapContext ldapContext;

    ADClient(final ProcuratSyncApplication application) {
        this.application = application;
    }

    public static ADClient createInstance(final ProcuratSyncApplication application) throws NamingException {
        final ADClient client = new ADClient(application);
        client.setup();
        return client;
    }

    @Override
    protected void setup() throws NamingException {
        final Hashtable<String, String> env = new Hashtable<>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.SECURITY_AUTHENTICATION, "simple");
        env.put(Context.SECURITY_PRINCIPAL, application.getConfiguration().getClients().getActiveDirectory().getPrincipal());
        env.put(Context.SECURITY_CREDENTIALS, application.getConfiguration().getClients().getActiveDirectory().getPassword());
        env.put(Context.PROVIDER_URL, application.getConfiguration().getClients().getActiveDirectory().getHost());
        env.put(Context.SECURITY_PROTOCOL, "SSL");
        env.put("java.naming.ldap.factory.socket", UnsecuredSSLSocketFactory.class.getName());

        ldapContext = new InitialLdapContext(env, null);
    }

    public ADUser findUserByEmployeeId(final int employeeId) throws NamingException {
        final String searchFilter = "(&(objectClass=person)(employeeID=" + employeeId + "))";
        return findUserByFilter(searchFilter);
    }

    public ADUser findUserByUsername(final String username) throws NamingException {
        final String searchFilter = "(&(objectClass=person)(sAMAccountName=" + username + "))";
        return findUserByFilter(searchFilter);
    }

    public ADUser findUserByFilter(final String filter) throws NamingException {
        final SearchControls searchControls = new SearchControls();
        searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);

        final NamingEnumeration<SearchResult> results = ldapContext.search(
                application.getConfiguration().getClients().getActiveDirectory().getUserDN(),
                filter,
                searchControls);
        if (!results.hasMoreElements()) {
            // Return null if there are none
            return null;
        }

        final SearchResult result = results.nextElement();
        if (results.hasMoreElements()) {
            // Throw if more than one user is found (data inconsistency)
            throw new IllegalStateException("Found more than one user for '" + filter + "'");
        }

        return new ADUser(result);
    }

    public List<ADUser> findAllUsers() throws NamingException {
        final String searchFilter = "(objectClass=person)";
        final SearchControls searchControls = new SearchControls();
        searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);

        final NamingEnumeration<SearchResult> results = ldapContext.search(
                application.getConfiguration().getClients().getActiveDirectory().getUserDN(),
                searchFilter,
                searchControls);
        if (!results.hasMoreElements()) {
            // Return empty list if there are none
            return Collections.emptyList();
        }

        final List<ADUser> users = new ArrayList<>();
        while (results.hasMoreElements()) {
            final SearchResult result = results.nextElement();
            users.add(new ADUser(result));
        }

        return users;
    }

    public void createUser(final String targetDN, final int employeeId, final String username, final String mail,
                           final String firstName, final String lastName, final String password, final String title,
                           final String office, final String description, final boolean mailAsUpn) throws NamingException {
        final Attributes attributes = new BasicAttributes();
        final Attribute objectClassAttribute = new BasicAttribute("objectClass");
        objectClassAttribute.add("person");
        objectClassAttribute.add("organizationalPerson");
        objectClassAttribute.add("user");
        attributes.put(objectClassAttribute);

        final String fullName = firstName + " " + lastName;
        final String dn = "cn=" + fullName + "," + targetDN;
        attributes.put("cn", fullName);
        attributes.put("displayName", fullName);
        attributes.put("givenName", firstName);
        attributes.put("sn", lastName);
        attributes.put("mail", mail);
        attributes.put("sAMAccountName", username);
        attributes.put("employeeID", Integer.toString(employeeId));
        attributes.put("title", title);
        attributes.put("physicalDeliveryOfficeName", office);
        attributes.put("description", description);

        // NORMAL_ACCOUNT = 0x200 (dec. 512)
        attributes.put("userAccountControl", "512");
        if (mailAsUpn) {
            attributes.put("userPrincipalName", mail);
        }
        attributes.put("unicodePwd", getPasswordBytes(password));
        // pwdLastSet = 0 (user has to change password on next login)
        attributes.put("pwdLastSet", "0");

        ldapContext.createSubcontext(dn, attributes);
        log.info("Created user '{}'", dn);
    }

    public void updateUser(final ADUser user, final String mail, final String firstName, final String lastName,
                           final String title, final String office, final String description, final boolean mailAsUpn) throws NamingException {

        final List<ModificationItem> modifications = new ArrayList<>();

        // TODO what about CN ? how to fully rename user ?

        // Mail
        if (!mail.equals(user.getMail())) {
            modifications.add(new ModificationItem(DirContext.REPLACE_ATTRIBUTE, new BasicAttribute("mail", mail)));
        }

        // UPN
        if (mailAsUpn && !mail.equals(user.getUserPrincipalName())) {
            modifications.add(new ModificationItem(DirContext.REPLACE_ATTRIBUTE, new BasicAttribute("userPrincipalName", mail)));
        }

        // First name
        if (!firstName.equals(user.getGivenName())) {
            modifications.add(new ModificationItem(DirContext.REPLACE_ATTRIBUTE, new BasicAttribute("givenName", firstName)));
        }

        // Last name
        if (!lastName.equals(user.getSn())) {
            modifications.add(new ModificationItem(DirContext.REPLACE_ATTRIBUTE, new BasicAttribute("sn", lastName)));
        }

        // Title
        if (!title.equals(user.getTitle())) {
            modifications.add(new ModificationItem(DirContext.REPLACE_ATTRIBUTE, new BasicAttribute("title", title)));
        }

        // Office
        if (!office.equals(user.getPhysicalDeliveryOfficeName())) {
            modifications.add(new ModificationItem(DirContext.REPLACE_ATTRIBUTE, new BasicAttribute("physicalDeliveryOfficeName", office)));
        }

        // Description
        if (!description.equals(user.getDescription())) {
            modifications.add(new ModificationItem(DirContext.REPLACE_ATTRIBUTE, new BasicAttribute("description", description)));
        }

        if (modifications.isEmpty()) {
            return;
        }

        ldapContext.modifyAttributes(user.getDn(), modifications.toArray(new ModificationItem[0]));
        log.info("Updated {} parameters of user '{}' (Username: {} | Id: {})", modifications.size(), user.getCn(), user.getSAMAccountName(), user.getEmployeeId());
    }

    public void disableUser(final ADUser user) throws NamingException {
        Preconditions.checkArgument(!user.isDisabled(), "user already disabled");

        // ACCOUNTDISABLE = 0x0002
        final BigInteger userAccountControl = user.getUserAccountControl().setBit(1);
        final Attribute attribute = new BasicAttribute("userAccountControl", String.valueOf(userAccountControl));
        final ModificationItem item = new ModificationItem(DirContext.REPLACE_ATTRIBUTE, attribute);
        ldapContext.modifyAttributes(user.getDn(), new ModificationItem[]{item});

        log.info("Disabled user '{}' (Username: {} | Id: {})", user.getCn(), user.getSAMAccountName(), user.getEmployeeId());
    }

    public void addToGroup(final ADUser user, final String groupDN) throws NamingException {
        final ModificationItem item = new ModificationItem(DirContext.ADD_ATTRIBUTE, new BasicAttribute("member", user.getDn()));
        ldapContext.modifyAttributes(groupDN, new ModificationItem[]{item});

        log.info("Added user '{}' (Username: {} | Id: {}) to group '{}'", user.getCn(), user.getSAMAccountName(), user.getEmployeeId(), groupDN);
    }

    public boolean isGroupMember(final ADUser user, final String groupDN) throws NamingException {
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
                    if (user.getDn().equals(memberDN)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private static byte[] getPasswordBytes(final String password) {
        final String quotedPassword = "\"" + password + "\"";
        return quotedPassword.getBytes(StandardCharsets.UTF_16LE);
    }

    @Override
    public void close() throws Exception {
        ldapContext.close();
    }
}
