package de.waldorfaugsburg.syncer.client.activedirectory.model;

import lombok.*;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchResult;
import java.math.BigInteger;

@NoArgsConstructor
@Data
@EqualsAndHashCode(of = "sAMAccountName")
@ToString
public final class ADUser {

    private String dn;
    private String cn;
    private String givenName;
    private String sn;
    private String mail;
    private String sAMAccountName;
    private String userPrincipalName;
    private Integer employeeId;
    private String title;
    private String physicalDeliveryOfficeName;
    private String description;

    private BigInteger userAccountControl;
    private Long pwdLastSet;

    public ADUser(final SearchResult result) throws NamingException {
        final Attributes attributes = result.getAttributes();
        dn = getAttributeStringValue(attributes, "distinguishedName");
        cn = getAttributeStringValue(attributes, "cn");
        givenName = getAttributeStringValue(attributes, "givenName");
        sn = getAttributeStringValue(attributes, "sn");
        mail = getAttributeStringValue(attributes, "mail");
        sAMAccountName = getAttributeStringValue(attributes, "sAMAccountName");
        userPrincipalName = getAttributeStringValue(attributes, "userPrincipalName");
        employeeId = getAttributeIntegerValue(attributes, "employeeID");
        title = getAttributeStringValue(attributes, "title");
        physicalDeliveryOfficeName = getAttributeStringValue(attributes, "physicalDeliveryOfficeName");
        description = getAttributeStringValue(attributes, "description");

        userAccountControl = getAttributeBigIntegerValue(attributes, "userAccountControl");
        pwdLastSet = getAttributeLongValue(attributes, "pwdLastSet");
    }

    public boolean isDisabled() {
        // ACCOUNTDISABLE = 0x0002
        return userAccountControl.testBit(1);
    }

    public boolean mustChangePassword() {
        return pwdLastSet == 0;
    }

    private String getAttributeStringValue(final Attributes attributes, final String attributeName) throws NamingException {
        final Attribute attribute = attributes.get(attributeName);
        return attribute != null ? (String) attribute.get() : null;
    }

    private Integer getAttributeIntegerValue(final Attributes attributes, final String attributeName) throws NamingException {
        final Attribute attribute = attributes.get(attributeName);
        return attribute != null ? Integer.parseInt((String) attribute.get()) : null;
    }

    private BigInteger getAttributeBigIntegerValue(final Attributes attributes, final String attributeName) throws NamingException {
        final Attribute attribute = attributes.get(attributeName);
        return attribute != null ? BigInteger.valueOf(Long.parseLong((String) attribute.get())) : null;
    }

    private Long getAttributeLongValue(final Attributes attributes, final String attributeName) throws NamingException {
        final Attribute attribute = attributes.get(attributeName);
        return attribute != null ? Long.parseLong((String) attribute.get()) : null;
    }

}
