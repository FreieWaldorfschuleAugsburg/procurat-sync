package de.waldorfaugsburg.syncer.module.ews.task.addressbook;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import de.waldorfaugsburg.syncer.SyncerApplication;
import de.waldorfaugsburg.syncer.module.ews.EWSModule;
import de.waldorfaugsburg.syncer.module.ews.model.ContactGroupModel;
import de.waldorfaugsburg.syncer.module.ews.model.ContactModel;
import de.waldorfaugsburg.syncer.module.procurat.ProcuratModule;
import de.waldorfaugsburg.syncer.module.procurat.model.ProcuratCommunication;
import de.waldorfaugsburg.syncer.module.procurat.model.ProcuratContactInformation;
import de.waldorfaugsburg.syncer.module.procurat.model.ProcuratGroupMembership;
import de.waldorfaugsburg.syncer.module.procurat.model.ProcuratPerson;
import de.waldorfaugsburg.syncer.task.AbstractScheduledTask;
import lombok.extern.slf4j.Slf4j;
import microsoft.exchange.webservices.data.core.enumeration.service.DeleteMode;
import microsoft.exchange.webservices.data.core.service.item.Contact;
import microsoft.exchange.webservices.data.core.service.item.ContactGroup;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class EWSAddressBookTask extends AbstractScheduledTask {

    private ProcuratModule procuratModule;
    private EWSModule ewsModule;

    public EWSAddressBookTask(final SyncerApplication application) {
        super(application, "ews_address_book.json", EWSAddressBookTaskConfiguration.class,
                ProcuratModule.class, EWSModule.class);
    }

    @Override
    public void run() throws Exception {
        procuratModule = getApplication().getModuleRegistry().getInstance(ProcuratModule.class);
        ewsModule = getApplication().getModuleRegistry().getInstance(EWSModule.class);

        log.info("Creating or updating contact groups");

        // Create or update contact groups
        final EWSAddressBookTaskConfiguration taskConfiguration = (EWSAddressBookTaskConfiguration) getConfiguration();
        final Map<Integer, EWSContactGroupConfiguration> groupConfigurationMap = new HashMap<>();
        for (final EWSContactGroupConfiguration group : taskConfiguration.getGroups()) {
            groupConfigurationMap.put(group.getId(), group);

            try {
                createOrUpdateContactGroup(group);
            } catch (final Exception e) {
                log.error("Error while creating/updating contact group (id: {}, name: {})", group.getId(), group.getDisplayName(), e);
            }
        }

        log.info("Deleting old contact groups");

        // Delete old contact groups
        for (final ContactGroup contactGroup : ewsModule.findAllContactGroups()) {
            try {
                final int groupId = ewsModule.readId(contactGroup);
                if (groupId == -1 || !groupConfigurationMap.containsKey(groupId)) {
                    contactGroup.delete(DeleteMode.HardDelete);
                    log.info("Deleted contact group (id={})", groupId);
                }
            } catch (final Exception e) {
                log.error("Error checking contact group deletion (id={})", contactGroup.getId(), e);
            }
        }

        log.info("Creating or updating contacts");

        // Create or update contacts
        final Map<Integer, ProcuratPerson> personMap = procuratModule.getAllPersonsMap();
        final Map<Integer, ProcuratGroupMembership> memberships = procuratModule.getRootGroupMembershipsMap();
        for (final ProcuratGroupMembership membership : memberships.values()) {
            try {
                final ProcuratPerson person = personMap.get(membership.getPersonId());
                createOrUpdateContact(person);
            } catch (final RuntimeException ignored) {
                log.debug("Runtime error while creating/updating contact (id: {})", membership.getPersonId());
            } catch (final Exception e) {
                log.error("Error while creating/updating contact (id: {})", membership.getPersonId(), e);
            }
        }

        log.info("Deleting old contacts");

        // Delete old contacts
        for (final Contact contact : ewsModule.findAllContacts()) {
            try {
                final int personId = ewsModule.readId(contact);
                if (personId == -1 || !memberships.containsKey(personId)) {
                    contact.delete(DeleteMode.HardDelete);
                    log.info("Deleted contact (id={})", personId);
                }
            } catch (final Exception e) {
                log.error("Error checking contact deletion (id={})", contact.getId(), e);
            }
        }
    }

    private void createOrUpdateContact(final ProcuratPerson person) throws Exception {
        final ContactModel model = new ContactModel();
        model.setId(person.getId());
        model.setFirstName(person.getFirstName());
        model.setLastName(person.getLastName());
        model.setFullName(person.getFullName());

        String privateEmail = null;
        String workEmail = null;
        for (final ProcuratContactInformation info : procuratModule.getContactInformationByPersonId(person.getId())) {
            if (workEmail == null && info.getOrder() == 1 && info.getMedium().equals("email") && info.getType().equals("work")) {
                workEmail = info.getContent();
                continue;
            }

            if (privateEmail == null && info.getOrder() == 1 && info.getMedium().equals("email") && info.getType().equals("private")) {
                privateEmail = info.getContent();
            }
        }

        model.setPrivateEmail(privateEmail);
        model.setWorkEmail(workEmail);
        model.setNote(constructContactNote(person));

        ewsModule.createOrUpdateContact(model);
    }

    private void createOrUpdateContactGroup(final EWSContactGroupConfiguration groupConfig) throws Exception {
        final ContactGroupModel model = new ContactGroupModel();
        model.setId(groupConfig.getId());
        model.setDisplayName(groupConfig.getDisplayName());

        final Multimap<Integer, String> personEmailTypeMap = ArrayListMultimap.create();

        // Add group members
        for (final Map.Entry<String, String> entry : groupConfig.getGroups().entrySet()) {
            final int groupId = Integer.parseInt(entry.getKey());
            final String emailType = entry.getValue();

            for (final ProcuratGroupMembership membership : procuratModule.getGroupMemberships(groupId)) {
                personEmailTypeMap.put(membership.getPersonId(), emailType);
            }
        }

        // Add group members correspondents
        for (final Map.Entry<String, String> entry : groupConfig.getCorrespondenceGroups().entrySet()) {
            final int groupId = Integer.parseInt(entry.getKey());
            final String emailType = entry.getValue();

            for (final ProcuratGroupMembership membership : procuratModule.getGroupMemberships(groupId)) {
                for (final ProcuratCommunication communication : procuratModule.getCommunicationsByPersonId(membership.getPersonId())) {
                    personEmailTypeMap.put(communication.getContactPersonId(), emailType);
                }
            }
        }

        // Add persons
        for (Map.Entry<String, String> entry : groupConfig.getPersons().entrySet()) {
            final int personId = Integer.parseInt(entry.getKey());
            final String emailType = entry.getValue();

            personEmailTypeMap.put(personId, emailType);
        }

        // Gather email addresses collected persons
        final Multimap<String, String> addresses = ArrayListMultimap.create();
        for (final Map.Entry<Integer, String> entry : personEmailTypeMap.entries()) {
            final ProcuratPerson person = procuratModule.getPersonById(entry.getKey());
            final String emailType = entry.getValue();

            for (final ProcuratContactInformation info : procuratModule.getContactInformationByPersonId(person.getId())) {
                if (info.getOrder() == 1 && info.getMedium().equals("email") && info.getType().equals(emailType)) {
                    addresses.put(person.getFullName(), info.getContent());
                }
            }
        }

        // Add one-off addresses
        for (final Map.Entry<String, String> entry : groupConfig.getOneOffAddresses().entrySet()) {
            addresses.put(entry.getKey(), entry.getValue());
        }

        model.setAddresses(addresses);
        ewsModule.createOrUpdateContactGroup(model);
    }

    private String constructContactNote(final ProcuratPerson person) {
        return "<html><head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\"></head><body>ID: " + person.getId() + " </body></html>";
    }
}
