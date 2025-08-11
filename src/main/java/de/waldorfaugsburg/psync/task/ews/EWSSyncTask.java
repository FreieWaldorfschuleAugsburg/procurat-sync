package de.waldorfaugsburg.psync.task.ews;

import com.cronutils.model.Cron;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.waldorfaugsburg.psync.ProcuratSyncApplication;
import de.waldorfaugsburg.psync.client.ews.EWSClient;
import de.waldorfaugsburg.psync.client.procurat.ProcuratClient;
import de.waldorfaugsburg.psync.client.procurat.model.*;
import de.waldorfaugsburg.psync.task.AbstractSyncTask;
import lombok.extern.slf4j.Slf4j;
import microsoft.exchange.webservices.data.core.service.item.Contact;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class EWSSyncTask extends AbstractSyncTask {

    public EWSSyncTask(final ProcuratSyncApplication application, final Cron interval, final JsonObject customData) {
        super(application, interval, customData);
    }

    @Override
    public void run() throws Exception {
        final ProcuratClient procuratClient = getApplication().getProcuratClient();

        final Multimap<String, Integer> personIdGroupMultimap = ArrayListMultimap.create();
        final JsonArray groups = getCustomData().get("groups").getAsJsonArray();
        for (final JsonElement element : groups) {
            List<Integer> persons = new ArrayList<>();

            final JsonObject groupObject = element.getAsJsonObject();
            final String name = groupObject.get("name").getAsString();

            // Add group members
            final JsonArray groupIds = groupObject.getAsJsonArray("groupIds");
            for (final JsonElement groupId : groupIds) {
                for (final ProcuratGroupMembership membership : procuratClient.getGroupMemberships(groupId.getAsInt())) {
                    persons.add(membership.getPersonId());
                }
            }

            // Add correspondence persons for group members
            final JsonArray correspondenceGroupIds = groupObject.getAsJsonArray("correspondenceGroupIds");
            for (final JsonElement groupId : correspondenceGroupIds) {
                for (final ProcuratGroupMembership membership : procuratClient.getGroupMemberships(groupId.getAsInt())) {
                    for (final ProcuratCommunication communication : procuratClient.getCommunicationsByPersonId(membership.getPersonId())) {
                        persons.add(communication.getContactPersonId());
                    }
                }
            }

            // Add specific persons
            final JsonArray personIds = groupObject.getAsJsonArray("personIds");
            for (final JsonElement personId : personIds) {
                persons.add(personId.getAsInt());
            }

            // Remove possible duplicate persons
            persons = persons.stream().distinct().collect(Collectors.toList());

            personIdGroupMultimap.putAll(name, persons);
        }

        log.info("Aggregated a total of {} persons in {} groups for synchronisation", personIdGroupMultimap.values().size(), personIdGroupMultimap.keySet().size());

        final EWSClient ewsClient = getApplication().getEwsClient();
        if (!ewsClient.deleteAllContacts()) {
            return;
        }

        final Map<Integer, Contact> contactMap = new HashMap<>();
        for (final String groupName : personIdGroupMultimap.keySet()) {
            final Collection<Integer> personIds = personIdGroupMultimap.get(groupName);
            log.info("Starting with group {} ({} members)", groupName, personIds.size());

            final List<Contact> contacts = new ArrayList<>();
            for (final Integer personId : personIds) {
                final ProcuratPerson person = procuratClient.getPersonById(personId);
                Contact contact = contactMap.get(personId);
                if (contact == null) {
                    contact = createContact(procuratClient, ewsClient, person);
                    contactMap.put(personId, contact);
                }
                contacts.add(contact);
            }

            Thread.sleep(2000);
            ewsClient.createContactGroup(groupName, contacts);
        }
    }

    private Contact createContact(final ProcuratClient procuratClient, final EWSClient ewsClient, final ProcuratPerson person) {
        String email = null;
        String homePhone = null;
        String mobilePhone = null;

        final List<ProcuratContactInformation> addressContactInfo = procuratClient.getContactInformationByAddressId(person.getAddressId());
        for (final ProcuratContactInformation addressInfo : addressContactInfo) {
            if (!addressInfo.getMedium().equals("telephone")) continue;

            homePhone = normalizePhoneNumber(addressInfo.getContent());
            break;
        }

        final List<ProcuratContactInformation> personContactInfo = procuratClient.getContactInformationByPersonId(person.getId());
        for (final ProcuratContactInformation personInfo : personContactInfo) {
            if (email == null && personInfo.getOrder() == 1 && personInfo.getMedium().equals("email")) {
                email = personInfo.getContent();
                continue;
            }

            if (mobilePhone == null && personInfo.getOrder() == 1 && personInfo.getMedium().equals("mobile")) {
                mobilePhone = normalizePhoneNumber(personInfo.getContent());
            }
        }

        final ProcuratAddress address = procuratClient.getAddressById(person.getAddressId());
        return ewsClient.createContact(person.getId(), person.getFirstName(), person.getLastName(), email, homePhone, mobilePhone, address.getCity(), address.getZip(), address.getStreet(), "");
    }

    private String normalizePhoneNumber(final String phoneNumber) {
        return phoneNumber.replaceAll("[^\\d.]+", "");
    }
}
