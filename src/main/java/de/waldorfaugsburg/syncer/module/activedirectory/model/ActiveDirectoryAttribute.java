package de.waldorfaugsburg.syncer.module.activedirectory.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ActiveDirectoryAttribute {

    DN("distinguishedName", true),
    CN("cn", true),
    GIVEN_NAME("givenName", true),
    SN("sn", true),
    DISPLAY_NAME("displayName", true),
    MAIL("mail", false),
    SAM_ACCOUNT_NAME("sAMAccountName", true),
    USER_PRINCIPAL_NAME("userPrincipalName", true),
    EMPLOYEE_ID("employeeID", true),
    TITLE("title", false),
    PHYSICAL_DELIVERY_OFFICE_NAME("physicalDeliveryOfficeName", false),
    DESCRIPTION("description", false),
    USER_ACCOUNT_CONTROL("userAccountControl", false),
    UNICODE_PWD("unicodePwd", false),
    PWD_LAST_SET("pwdLastSet", false);

    private final String ldapAttributeName;
    private final boolean staticAttribute;
}
