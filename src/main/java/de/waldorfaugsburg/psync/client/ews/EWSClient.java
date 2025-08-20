package de.waldorfaugsburg.psync.client.ews;

import com.microsoft.aad.msal4j.*;
import de.waldorfaugsburg.psync.ProcuratSyncApplication;
import lombok.extern.slf4j.Slf4j;
import microsoft.exchange.webservices.data.core.ExchangeService;
import microsoft.exchange.webservices.data.core.PropertySet;
import microsoft.exchange.webservices.data.core.enumeration.misc.ConnectingIdType;
import microsoft.exchange.webservices.data.core.enumeration.misc.ExchangeVersion;
import microsoft.exchange.webservices.data.core.enumeration.property.*;
import microsoft.exchange.webservices.data.core.enumeration.service.ConflictResolutionMode;
import microsoft.exchange.webservices.data.core.enumeration.service.DeleteMode;
import microsoft.exchange.webservices.data.core.service.folder.Folder;
import microsoft.exchange.webservices.data.core.service.item.Contact;
import microsoft.exchange.webservices.data.core.service.item.ContactGroup;
import microsoft.exchange.webservices.data.misc.ImpersonatedUserId;
import microsoft.exchange.webservices.data.property.complex.*;
import microsoft.exchange.webservices.data.property.definition.ExtendedPropertyDefinition;
import microsoft.exchange.webservices.data.search.ItemView;
import microsoft.exchange.webservices.data.search.filter.SearchFilter;

import java.net.URI;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
public final class EWSClient {

    private static final String EWS_URL = "https://outlook.office365.com/EWS/Exchange.asmx";
    private static final String EWS_SCOPE = "https://outlook.office365.com/.default";
    private static final String EWS_AUTHORITY = "https://login.microsoftonline.com/%s/";
    private static final ExtendedPropertyDefinition PROCURAT_ID_PROPERTY;

    static {
        try {
            PROCURAT_ID_PROPERTY = new ExtendedPropertyDefinition(UUID.fromString("757f160d-68cf-4dbb-8c5f-feab33b86145"), "ProcuratId", MapiPropertyType.Integer);
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    private final String clientId;
    private final String tenantId;
    private final String clientSecret;
    private final String impersonatedUserId;

    private ExchangeService service;
    private FolderId contactFolderId;
    private Folder contactFolder;

    public EWSClient(final ProcuratSyncApplication application) {
        this.clientId = application.getConfiguration().getClients().getEws().getClientId();
        this.tenantId = application.getConfiguration().getClients().getEws().getTenantId();
        this.clientSecret = application.getConfiguration().getClients().getEws().getClientSecret();
        this.impersonatedUserId = application.getConfiguration().getClients().getEws().getImpersonatedUserId();

        final String contactFolderId = application.getConfiguration().getClients().getEws().getContactFolderId();
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

    public Contact findContactByPersonId(final int personId) {
        try {
            return (Contact) contactFolder.findItems(new SearchFilter.IsEqualTo(PROCURAT_ID_PROPERTY, personId), new ItemView(1)).getItems().getFirst();
        } catch (final Exception e) {
            log.error("Error finding contact by person id {}", personId);
            return null;
        }
    }

    public Contact createContact(final int personId, final String firstName, final String lastName, final String privateEmail, final String workEmail, final String homePhone, final String mobilePhone, final String city, final String postalCode, final String street, final String note) {
        final String fullName = lastName + " " + firstName;

        try {
            final Contact contact = new Contact(service);
            contact.setGivenName(firstName);
            contact.setSurname(lastName);
            contact.setDisplayName(fullName);

            if (privateEmail != null) {
                contact.getEmailAddresses().setEmailAddress(EmailAddressKey.EmailAddress1, new EmailAddress(fullName, privateEmail));
            }

            if (workEmail != null) {
                contact.getEmailAddresses().setEmailAddress(EmailAddressKey.EmailAddress2, new EmailAddress(fullName, workEmail));
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
            contact.setExtendedProperty(PROCURAT_ID_PROPERTY, personId);
            contact.save(contactFolderId);
            log.info("Created contact (name: {})", fullName);
            return contact;
        } catch (final Exception e) {
            log.error("Error adding contact {}", fullName, e);
            return null;
        }
    }

    public void createContactGroup(final String groupName, final Map<String, String> displayNameAddressMap) {
        try {
            final ContactGroup contactGroup = new ContactGroup(service);
            contactGroup.setDisplayName(groupName);
            contactGroup.save(contactFolderId);

            displayNameAddressMap.forEach((displayName, address) -> {
                try {
                    contactGroup.getMembers().addOneOff(displayName, address);
                    log.info("Add address {} <{}> to group {}", displayName, address, groupName);
                } catch (final Exception e) {
                    log.error("Error adding {} <{}> to group {}", displayName, address, groupName, e);
                }
            });

            contactGroup.update(ConflictResolutionMode.AlwaysOverwrite);
            log.info("Created contact group {} with {} contacts", groupName, displayNameAddressMap.size());
        } catch (final Exception e) {
            log.error("Error adding contact group {}", groupName, e);
        }
    }
}
