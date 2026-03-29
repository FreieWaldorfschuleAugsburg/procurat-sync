package de.waldorfaugsburg.syncer.module.activedirectory.model;

import javax.naming.directory.Attributes;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;

public record ActiveDirectoryUser(Attributes attributes) {

    public <T> void setAttribute(final ActiveDirectoryAttribute attribute, final T value) {
        attributes.put(attribute.getLdapAttributeName(), value);
    }

    public <T> T getAttribute(final ActiveDirectoryAttribute attribute) {
        return (T) attributes.get(attribute.getLdapAttributeName());
    }

    public void setPassword(final String password) {
        final String quotedPassword = "\"" + password + "\"";
        attributes.put(ActiveDirectoryAttribute.UNICODE_PWD.getLdapAttributeName(), quotedPassword.getBytes(StandardCharsets.UTF_16LE));
    }

    public void setDisabled(final boolean disabled) {
        // ACCOUNTDISABLE = 0x0002
        BigInteger userAccountControl = getAttribute(ActiveDirectoryAttribute.USER_ACCOUNT_CONTROL);
        if (disabled) {
            userAccountControl = userAccountControl.setBit(1);
        } else {
            userAccountControl = userAccountControl.clearBit(1);
        }

        attributes.put(ActiveDirectoryAttribute.USER_ACCOUNT_CONTROL.getLdapAttributeName(), String.valueOf(userAccountControl));
    }

    public boolean isDisabled() {
        // ACCOUNTDISABLE = 0x0002
        final BigInteger userAccountControl = getAttribute(ActiveDirectoryAttribute.USER_ACCOUNT_CONTROL);
        return userAccountControl.testBit(1);
    }

    public boolean mustChangePassword() {
        return (long) getAttribute(ActiveDirectoryAttribute.PWD_LAST_SET) == 0;
    }
}
