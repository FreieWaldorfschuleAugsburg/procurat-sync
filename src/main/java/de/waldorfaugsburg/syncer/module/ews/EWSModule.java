package de.waldorfaugsburg.syncer.module.ews;

import com.microsoft.aad.msal4j.*;
import de.waldorfaugsburg.syncer.SyncerApplication;
import de.waldorfaugsburg.syncer.module.AbstractModule;
import de.waldorfaugsburg.syncer.module.ews.model.ContactGroupModel;
import de.waldorfaugsburg.syncer.module.ews.model.ContactModel;
import lombok.extern.slf4j.Slf4j;
import microsoft.exchange.webservices.data.core.ExchangeService;
import microsoft.exchange.webservices.data.core.PropertySet;
import microsoft.exchange.webservices.data.core.enumeration.misc.ConnectingIdType;
import microsoft.exchange.webservices.data.core.enumeration.misc.ExchangeVersion;
import microsoft.exchange.webservices.data.core.enumeration.property.*;
import microsoft.exchange.webservices.data.core.enumeration.service.ConflictResolutionMode;
import microsoft.exchange.webservices.data.core.service.folder.Folder;
import microsoft.exchange.webservices.data.core.service.item.Contact;
import microsoft.exchange.webservices.data.core.service.item.ContactGroup;
import microsoft.exchange.webservices.data.core.service.item.Item;
import microsoft.exchange.webservices.data.core.service.schema.FolderSchema;
import microsoft.exchange.webservices.data.core.service.schema.ItemSchema;
import microsoft.exchange.webservices.data.misc.ImpersonatedUserId;
import microsoft.exchange.webservices.data.misc.OutParam;
import microsoft.exchange.webservices.data.property.complex.EmailAddress;
import microsoft.exchange.webservices.data.property.complex.FolderId;
import microsoft.exchange.webservices.data.property.complex.GroupMember;
import microsoft.exchange.webservices.data.property.complex.MessageBody;
import microsoft.exchange.webservices.data.property.definition.ExtendedPropertyDefinition;
import microsoft.exchange.webservices.data.search.FindItemsResults;
import microsoft.exchange.webservices.data.search.ItemView;
import microsoft.exchange.webservices.data.search.filter.SearchFilter;

import java.net.URI;
import java.util.*;

@Slf4j
public class EWSModule extends AbstractModule {

    private static final String EWS_URL = "https://outlook.office365.com/EWS/Exchange.asmx";
    private static final String EWS_SCOPE = "https://outlook.office365.com/.default";
    private static final String EWS_AUTHORITY = "https://login.microsoftonline.com/%s/";
    private static final int ITEMS_PER_PAGE = 50;

    public static final ExtendedPropertyDefinition CONTACT_ID_PROPERTY;
    public static final ExtendedPropertyDefinition GROUP_ID_PROPERTY;

    static {
        try {
            final UUID propertySetId = UUID.fromString("757f160d-68cf-4dbb-8c5f-feab33b86145");
            CONTACT_ID_PROPERTY = new ExtendedPropertyDefinition(propertySetId, "ContactSyncId", MapiPropertyType.Integer);
            GROUP_ID_PROPERTY = new ExtendedPropertyDefinition(propertySetId, "GroupSyncId", MapiPropertyType.Integer);
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    private EWSConfig config;
    private ExchangeService service;
    private Folder contactFolder;

    public EWSModule(final SyncerApplication application) {
        super(application);
    }

    @Override
    public void init() throws Exception {
        config = getApplication().loadConfiguration("ews.json", EWSConfig.class);

        final IClientCredential credential = ClientCredentialFactory.createFromSecret(config.getClientSecret());
        final ConfidentialClientApplication confidentialClientApplication = ConfidentialClientApplication.builder(config.getClientId(), credential).authority(String.format(EWS_AUTHORITY, config.getTenantId())).build();
        final ClientCredentialParameters parameters = ClientCredentialParameters.builder(Set.of(EWS_SCOPE)).build();
        final IAuthenticationResult result = confidentialClientApplication.acquireToken(parameters).join();

        service = new ExchangeService(ExchangeVersion.Exchange2010_SP2);
        service.getHttpHeaders().put("Authorization", "Bearer " + result.accessToken());
        service.setUrl(new URI(EWS_URL));
        service.setImpersonatedUserId(new ImpersonatedUserId(ConnectingIdType.SmtpAddress, config.getImpersonatedUserId()));
        service.setTimeout(20_000);

        contactFolder = service.bindToFolder(FolderId.getFolderIdFromString(config.getContactFolderId()), PropertySet.IdOnly);
    }

    @Override
    public void destroy() throws Exception {
        service.close();
    }

    public void createOrUpdateContact(final ContactModel model) throws Exception {
        if (model.getPrivateEmail() == null && model.getWorkEmail() == null)
            throw new RuntimeException("skipping person without email");

        boolean newContact = false;
        Contact contact = findContact(model.getId());
        if (contact == null) {
            contact = new Contact(service);
            contact.setExtendedProperty(CONTACT_ID_PROPERTY, model.getId());
            newContact = true;
        }

        boolean changes = false;

        if (newContact || !model.getFirstName().equals(contact.getGivenName())) {
            contact.setGivenName(model.getFirstName());
            changes = true;
        }

        if (newContact || !model.getLastName().equals(contact.getSurname())) {
            contact.setSurname(model.getLastName());
            changes = true;
        }

        if (newContact || !model.getFullName().equals(contact.getDisplayName())) {
            contact.setDisplayName(model.getFullName());
            changes = true;
        }

        final String newPrivateEmail = model.getPrivateEmail();
        if (newPrivateEmail != null) {
            final OutParam<EmailAddress> currentPrivateEmail = new OutParam<>();
            final boolean hasPrivateEmail = contact.getEmailAddresses().tryGetValue(EmailAddressKey.EmailAddress1, currentPrivateEmail);
            if (newContact || (hasPrivateEmail && !currentPrivateEmail.getParam().getAddress().equals(newPrivateEmail))) {
                contact.getEmailAddresses().setEmailAddress(EmailAddressKey.EmailAddress1, new EmailAddress(model.getFullName(), newPrivateEmail));
                changes = true;
            }
        }

        final String newWorkEmail = model.getWorkEmail();
        if (newWorkEmail != null) {
            final OutParam<EmailAddress> currentWorkEmail = new OutParam<>();
            final boolean hasWorkEmail = contact.getEmailAddresses().tryGetValue(EmailAddressKey.EmailAddress2, currentWorkEmail);
            if (newContact || (hasWorkEmail && !currentWorkEmail.getParam().getAddress().equals(newWorkEmail))) {
                contact.getEmailAddresses().setEmailAddress(EmailAddressKey.EmailAddress2, new EmailAddress(model.getFullName(), newWorkEmail));
                changes = true;
            }
        }

        final OutParam<Object> currentBody = new OutParam<>();
        final boolean hasBody = contact.tryGetProperty(ItemSchema.Body, currentBody);
        if (newContact || (hasBody && !MessageBody.getStringFromMessageBody((MessageBody) currentBody.getParam()).equals(model.getNote()))) {
            contact.setBody(MessageBody.getMessageBodyFromText(model.getNote()));
            changes = true;
        }

        if (newContact) {
            contact.save(contactFolder.getId());
            log.info("Created new contact (id: {}, name: {})", model.getId(), contact.getDisplayName());
        } else if (changes) {
            contact.update(ConflictResolutionMode.AlwaysOverwrite);
            log.info("Updated contact (id: {}, name: {})", model.getId(), contact.getDisplayName());
        }
    }

    public void createOrUpdateContactGroup(final ContactGroupModel model) throws Exception {
        boolean newGroup = false;
        ContactGroup group = findContactGroup(model.getId());
        if (group == null) {
            group = new ContactGroup(service);
            group.setExtendedProperty(GROUP_ID_PROPERTY, model.getId());
            newGroup = true;
        }

        group.setDisplayName(model.getDisplayName());

        if (!newGroup) {
            for (final GroupMember item : Set.copyOf(group.getMembers().getItems())) {
                group.getMembers().remove(item);
            }
            group.update(ConflictResolutionMode.AlwaysOverwrite);
        }

        for (final Map.Entry<String, String> entry : model.getAddresses().entries()) {
            group.getMembers().addOneOff(entry.getKey(), entry.getValue());
        }

        if (newGroup) {
            group.save(contactFolder.getId());
            log.info("Created new contact group (id: {}, name: {}, members: {})", model.getId(), group.getDisplayName(), group.getMembers().getCount());
            return;
        }

        group.update(ConflictResolutionMode.AlwaysOverwrite);
        log.info("Updated contact group (id: {}, name: {}, members: {})", model.getId(), group.getDisplayName(), group.getMembers().getCount());
    }

    public List<Contact> findAllContacts() throws Exception {
        return findAllItems(Contact.class);
    }

    public Contact findContact(final int id) throws Exception {
        final FindItemsResults<Item> results = contactFolder.findItems(new SearchFilter.IsEqualTo(CONTACT_ID_PROPERTY, id), new ItemView(1));
        if (results.getTotalCount() > 0) {
            final Contact contact = (Contact) results.getItems().getFirst();
            contact.load();
            return contact;
        }

        return null;
    }

    public List<ContactGroup> findAllContactGroups() throws Exception {
        return findAllItems(ContactGroup.class);
    }

    public ContactGroup findContactGroup(final int id) throws Exception {
        final FindItemsResults<Item> results = contactFolder.findItems(new SearchFilter.IsEqualTo(GROUP_ID_PROPERTY, id), new ItemView(1));
        if (results.getTotalCount() > 0) {
            final ContactGroup contactGroup = (ContactGroup) results.getItems().getFirst();
            contactGroup.load();
            return contactGroup;
        }

        return null;
    }

    public int readId(final Item item) throws Exception {
        final OutParam<Integer> outParam = new OutParam<>();
        if (item instanceof Contact contact) {
            contact.load(new PropertySet(CONTACT_ID_PROPERTY));
            contact.getExtendedProperties().tryGetValue(Integer.class, CONTACT_ID_PROPERTY, outParam);
        } else if (item instanceof ContactGroup contactGroup) {
            contactGroup.load(new PropertySet(GROUP_ID_PROPERTY));
            contactGroup.getExtendedProperties().tryGetValue(Integer.class, GROUP_ID_PROPERTY, outParam);
        }

        return outParam.getParam() != null ? outParam.getParam() : -1;
    }

    public <T extends Item> List<T> findAllItems(final Class<T> itemClass) throws Exception {
        contactFolder.load(new PropertySet(BasePropertySet.IdOnly, FolderSchema.TotalCount));

        final int totalCount = contactFolder.getTotalCount();
        final int pageCount = totalCount / ITEMS_PER_PAGE;

        final List<T> contacts = new ArrayList<>();
        for (int offset = 0; offset <= pageCount; offset += ITEMS_PER_PAGE) {
            for (final Item item : contactFolder.findItems(new ItemView(ITEMS_PER_PAGE, offset)).getItems()) {
                if (itemClass.isInstance(item)) {
                    contacts.add((T) item);
                }
            }
        }

        return contacts;
    }
}
