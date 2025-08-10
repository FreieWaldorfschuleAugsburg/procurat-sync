package de.waldorfaugsburg.psync.client.ews;

import com.microsoft.aad.msal4j.*;
import lombok.extern.slf4j.Slf4j;
import microsoft.exchange.webservices.data.core.ExchangeService;
import microsoft.exchange.webservices.data.core.PropertySet;
import microsoft.exchange.webservices.data.core.enumeration.misc.ConnectingIdType;
import microsoft.exchange.webservices.data.core.enumeration.misc.ExchangeVersion;
import microsoft.exchange.webservices.data.core.enumeration.property.*;
import microsoft.exchange.webservices.data.core.enumeration.service.DeleteMode;
import microsoft.exchange.webservices.data.core.service.folder.Folder;
import microsoft.exchange.webservices.data.core.service.item.Contact;
import microsoft.exchange.webservices.data.misc.ImpersonatedUserId;
import microsoft.exchange.webservices.data.property.complex.*;

import java.net.URI;
import java.util.Set;

@Slf4j
public final class EWSClient {

    private static final String EWS_URL = "https://outlook.office365.com/EWS/Exchange.asmx";
    private static final String EWS_SCOPE = "https://outlook.office365.com/.default";
    private static final String EWS_AUTHORITY = "https://login.microsoftonline.com/%s/";

    private final String clientId;
    private final String tenantId;
    private final String clientSecret;
    private final String impersonatedUserId;

    private ExchangeService service;
    private FolderId contactFolderId;
    private Folder contactFolder;

    public EWSClient(final String clientId, final String tenantId, final String clientSecret, final String contactFolderId, final String impersonatedUserId) {
        this.clientId = clientId;
        this.tenantId = tenantId;
        this.clientSecret = clientSecret;
        this.impersonatedUserId = impersonatedUserId;

        try {
            this.contactFolderId = FolderId.getFolderIdFromString(contactFolderId);
        } catch (final Exception e) {
            log.error("Invalid contact folder id {}", contactFolderId, e);
            return;
        }

        setup();
    }

    private void setup() {
        try {
            final IClientCredential credential = ClientCredentialFactory.createFromSecret(clientSecret);
            final ConfidentialClientApplication confidentialClientApplication = ConfidentialClientApplication.builder(clientId, credential).authority(String.format(EWS_AUTHORITY, tenantId)).build();

            final ClientCredentialParameters parameters = ClientCredentialParameters.builder(Set.of(EWS_SCOPE)).build();
            final IAuthenticationResult result = confidentialClientApplication.acquireToken(parameters).join();
            service = new ExchangeService(ExchangeVersion.Exchange2010_SP2);
            service.getHttpHeaders().put("Authorization", "Bearer " + result.accessToken());
            service.setUrl(new URI(EWS_URL));
            service.setImpersonatedUserId(new ImpersonatedUserId(ConnectingIdType.SmtpAddress, impersonatedUserId));
            contactFolder = service.bindToFolder(contactFolderId, PropertySet.IdOnly);
        } catch (final Exception e) {
            log.error("Error during client setup", e);
        }
    }

    public boolean deleteAllContacts() {
        try {
            contactFolder.empty(DeleteMode.HardDelete, false);
            return true;
        } catch (final Exception e) {
            log.error("Error deleting all contacts", e);
            return false;
        }
    }

    public void addContact(final String firstName, final String lastName, final String email, final String homePhone, final String mobilePhone, final String city, final String postalCode, final String street, final String note) {
        final String fullName = firstName + " " + lastName;

        try {
            final Contact contact = new Contact(service);
            contact.setGivenName(firstName);
            contact.setSurname(lastName);
            contact.setDisplayName(fullName);

            if (email != null) {
                contact.getEmailAddresses().setEmailAddress(EmailAddressKey.EmailAddress1, new EmailAddress(fullName, email));
            }

            if (homePhone != null) {
                contact.getPhoneNumbers().setPhoneNumber(PhoneNumberKey.HomePhone, homePhone);
            }

            if (mobilePhone != null) {
                contact.getPhoneNumbers().setPhoneNumber(PhoneNumberKey.MobilePhone, mobilePhone);
            }

            final PhysicalAddressEntry physicalAddressEntry = new PhysicalAddressEntry();
            physicalAddressEntry.setCity(city);
            physicalAddressEntry.setPostalCode(postalCode);
            physicalAddressEntry.setStreet(street);
            contact.getPhysicalAddresses().setPhysicalAddress(PhysicalAddressKey.Home, physicalAddressEntry);
            contact.setPostalAddressIndex(PhysicalAddressIndex.Home);
            contact.setBody(new MessageBody(note));
            contact.save(contactFolderId);
        } catch (final Exception e) {
            log.error("Error adding contact {}", fullName, e);
        }
    }
}
