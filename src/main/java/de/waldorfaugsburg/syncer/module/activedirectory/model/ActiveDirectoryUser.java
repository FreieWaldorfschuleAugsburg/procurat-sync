package de.waldorfaugsburg.syncer.module.activedirectory.model;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;

public record ActiveDirectoryUser(Attributes attributes) {

    public ActiveDirectoryUser() {
        this(new BasicAttributes());
        
        final Attribute objectClassAttribute = new BasicAttribute("objectClass");
        objectClassAttribute.add("person");
        objectClassAttribute.add("organizationalPerson");
        objectClassAttribute.add("user");
        attributes.put(objectClassAttribute);
    }

    public <T> void setAttribute(final ActiveDirectoryAttribute activeDirectoryAttribute, final T value) {
        attributes.put(activeDirectoryAttribute.getLdapAttributeName(), value);
    }

    public <T> T getAttribute(final ActiveDirectoryAttribute activeDirectoryAttribute) throws NamingException {
        final Attribute attribute = attributes.get(activeDirectoryAttribute.getLdapAttributeName());
        if (attribute == null) {
            return null;
        }

        return (T) attribute.get();
    }

    public void setPassword(final String password) {
        final String quotedPassword = "\"" + password + "\"";
        attributes.put(ActiveDirectoryAttribute.UNICODE_PWD.getLdapAttributeName(), quotedPassword.getBytes(StandardCharsets.UTF_16LE));
    }

    public void setDisabled(final boolean disabled) throws NamingException {
        // ACCOUNTDISABLE = 0x0002
        BigInteger userAccountControl = getAttribute(ActiveDirectoryAttribute.USER_ACCOUNT_CONTROL);
        if (disabled) {
            userAccountControl = userAccountControl.setBit(1);
        } else {
            userAccountControl = userAccountControl.clearBit(1);
        }

        attributes.put(ActiveDirectoryAttribute.USER_ACCOUNT_CONTROL.getLdapAttributeName(), String.valueOf(userAccountControl));
    }

    public boolean isDisabled() throws NamingException {
        // ACCOUNTDISABLE = 0x0002
        final BigInteger userAccountControl = getAttribute(ActiveDirectoryAttribute.USER_ACCOUNT_CONTROL);
        return userAccountControl.testBit(1);
    }

    public boolean mustChangePassword() throws NamingException {
        return (long) getAttribute(ActiveDirectoryAttribute.PWD_LAST_SET) == 0;
    }
}
