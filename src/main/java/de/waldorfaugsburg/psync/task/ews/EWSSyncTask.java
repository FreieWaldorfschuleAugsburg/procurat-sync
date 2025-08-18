package de.waldorfaugsburg.psync.task.ews;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import de.waldorfaugsburg.psync.ProcuratSyncApplication;
import de.waldorfaugsburg.psync.client.ews.EWSClient;
import de.waldorfaugsburg.psync.client.procurat.ProcuratClient;
import de.waldorfaugsburg.psync.client.procurat.model.*;
import de.waldorfaugsburg.psync.task.AbstractSyncTask;
import lombok.extern.slf4j.Slf4j;
import microsoft.exchange.webservices.data.core.enumeration.property.EmailAddressKey;
import microsoft.exchange.webservices.data.core.service.item.Contact;
import microsoft.exchange.webservices.data.misc.OutParam;
import microsoft.exchange.webservices.data.property.complex.EmailAddress;

import java.util.*;

@Slf4j
public class EWSSyncTask extends AbstractSyncTask<EWSSyncTaskConfiguration> {

    public EWSSyncTask(final ProcuratSyncApplication application, final EWSSyncTaskConfiguration configuration) {
        super(application, configuration);
    }

    @Override
    public void run() throws Exception {
        final ProcuratClient procuratClient = getApplication().getProcuratClient();
        final List<ProcuratGroupMembership> rootGroupMemberships = procuratClient.getRootGroupMemberships();

        final Multimap<EWSSyncTaskConfiguration.ContactGroup, EWSSyncTaskConfiguration.Selector> selectorGroupMap = ArrayListMultimap.create();
        for (final EWSSyncTaskConfiguration.ContactGroup group : getConfiguration().getGroups()) {
            selectorGroupMap.putAll(group, accumulateSelectors(procuratClient, group).stream().distinct().toList());
        }

        final long personCount = selectorGroupMap.values().stream().distinct().count();
        log.info("Aggregated a total of {} persons in {} groups for synchronisation", personCount, selectorGroupMap.keySet().size());

        final EWSClient ewsClient = getApplication().getEwsClient();
        if (!ewsClient.deleteAllContacts()) {
            throw new IllegalStateException("Could not delete contacts");
        }

        final Map<Integer, Contact> contactMap = new HashMap<>();
        for (final EWSSyncTaskConfiguration.ContactGroup group : selectorGroupMap.keySet()) {
            final Collection<EWSSyncTaskConfiguration.Selector> selectors = selectorGroupMap.get(group);
            log.info("Starting with group {} ({} members)", group.getName(), selectors.size());

            final Map<String, String> contactEmailMap = new HashMap<>();
            for (final EWSSyncTaskConfiguration.Selector selector : selectors) {
                final ProcuratPerson person = procuratClient.getPersonById(selector.getId());
                Contact contact = contactMap.get(person.getId());
                if (contact == null) {
                    contact = createContact(procuratClient, ewsClient, rootGroupMemberships, person);
                    contactMap.put(person.getId(), contact);
                }

                final String emailTypeString = selector.getEmailType();
                if (emailTypeString.equals("private")) {
                    final OutParam<EmailAddress> outParam = new OutParam<>();
                    if (contact.getEmailAddresses().tryGetValue(EmailAddressKey.EmailAddress1, outParam)) {
                        contactEmailMap.put(contact.getDisplayName(), outParam.getParam().getAddress());
                    } else if (contact.getEmailAddresses().tryGetValue(EmailAddressKey.EmailAddress2, outParam)) {
                        contactEmailMap.put(contact.getDisplayName(), outParam.getParam().getAddress());
                        recordDeviation("Fallback to work email for '%s' (Id: %s) for membership in '%s'", contact.getDisplayName(), person.getId(), group.getName());
                    } else {
                        recordDeviation("Could not find private email for '%s' (Id: %s) for membership in '%s'", contact.getDisplayName(), person.getId(), group.getName());
                    }
                } else if (emailTypeString.equals("work")) {
                    final OutParam<EmailAddress> outParam = new OutParam<>();
                    if (contact.getEmailAddresses().tryGetValue(EmailAddressKey.EmailAddress2, outParam)) {
                        contactEmailMap.put(contact.getDisplayName(), outParam.getParam().getAddress());
                    } else {
                        recordDeviation("Could not find work email for '%s' (Id: %s) for membership in '%s'", person.getLastName() + " " + person.getFirstName(), person.getId(), group.getName());
                    }
                }
            }

            // Add extra addresses
            final Map<String, String> extraAddresses = group.getExtraAddresses();
            if (extraAddresses != null) {
                contactEmailMap.putAll(group.getExtraAddresses());
            }

            if (contactEmailMap.size() <= 1) {
                recordDeviation("%s contacts found for group %s", contactEmailMap.size(), group.getName());
            }

            Thread.sleep(2000);
            ewsClient.createContactGroup(group.getName(), contactEmailMap);
        }
    }

    private Contact createContact(final ProcuratClient procuratClient, final EWSClient ewsClient, final List<ProcuratGroupMembership> rootMemberships, final ProcuratPerson person) {
        String workEmail = null;
        String privateEmail = null;
        String homePhone = null;
        String mobilePhone = null;

        // Address contact information
        final List<ProcuratContactInformation> addressContactInfo = procuratClient.getContactInformationByAddressId(person.getAddressId());
        for (final ProcuratContactInformation addressInfo : addressContactInfo) {
            if (!addressInfo.getMedium().equals("telephone")) continue;

            homePhone = normalizePhoneNumber(addressInfo.getContent());
            break;
        }

        // Personal contact information
        final List<ProcuratContactInformation> personContactInfo = procuratClient.getContactInformationByPersonId(person.getId());
        for (final ProcuratContactInformation personInfo : personContactInfo) {
            if (workEmail == null && personInfo.getOrder() == 1 && personInfo.getMedium().equals("email") && personInfo.getType().equals("work")) {
                workEmail = personInfo.getContent();
                continue;
            }

            if (privateEmail == null && personInfo.getOrder() == 1 && personInfo.getMedium().equals("email") && personInfo.getType().equals("private")) {
                privateEmail = personInfo.getContent();
                continue;
            }

            if (mobilePhone == null && personInfo.getOrder() == 1 && personInfo.getMedium().equals("mobile")) {
                mobilePhone = normalizePhoneNumber(personInfo.getContent());
            }
        }

        // Note (in contact body)
        final StringBuilder noteBuilder = new StringBuilder();
        noteBuilder.append("<h1>Person</h1>");
        noteBuilder.append("Personennummer: ").append(person.getId()).append("<br>");
        noteBuilder.append("Anschriftnummer: ").append(person.getAddressId()).append("<br>");

        boolean parent = false;
        if (person.getFamilyRole().equals("mother")) {
            parent = true;
            noteBuilder.append("<h1>Familie</h1>");
            noteBuilder.append("Familienrolle: Mutter").append("<br>");
        }

        if (person.getFamilyRole().equals("father")) {
            parent = true;
            noteBuilder.append("<h1>Familie</h1>");
            noteBuilder.append("Familienrolle: Vater").append("<br>");
        }

        if (parent) {
            noteBuilder.append("Weitere Familienmitglieder:");
            noteBuilder.append("<ul>");
            final List<ProcuratPerson> persons = procuratClient.getPersonsByFamilyId(person.getFamilyId());
            for (final ProcuratPerson familyPerson : persons) {
                if (familyPerson.getId() == person.getId()) continue;
                if (!procuratClient.isPersonActiveMember(rootMemberships, familyPerson)) continue;

                final String namedGroupName = procuratClient.getNamedGroupName(familyPerson.getId());
                if (namedGroupName == null) continue;

                noteBuilder.append("<li>");

                switch (familyPerson.getFamilyRole()) {
                    case "mother" -> noteBuilder.append("Mutter: ");
                    case "father" -> noteBuilder.append("Vater: ");
                    case "child" -> noteBuilder.append("Kind: ");
                }

                noteBuilder.append(familyPerson.getFirstName())
                        .append(" ")
                        .append(familyPerson.getLastName())
                        .append(" (").append(namedGroupName).append(")")
                        .append("</li>");
            }
            noteBuilder.append("</ul>");
        }

        final ProcuratAddress address = procuratClient.getAddressById(person.getAddressId());
        return ewsClient.createContact(person.getId(), person.getFirstName(), person.getLastName(), privateEmail, workEmail, homePhone, mobilePhone, address.getCity(), address.getZip(), address.getStreet(), noteBuilder.toString());
    }

    private List<EWSSyncTaskConfiguration.Selector> accumulateSelectors(final ProcuratClient client, final EWSSyncTaskConfiguration.ContactGroup group) {
        final List<EWSSyncTaskConfiguration.Selector> selectors = new ArrayList<>();

        // Add group members
        for (final EWSSyncTaskConfiguration.Selector selector : group.getGroups()) {
            for (final ProcuratGroupMembership membership : client.getGroupMemberships(selector.getId())) {
                selectors.add(new EWSSyncTaskConfiguration.Selector(selector, membership.getPersonId()));
            }
        }

        // Add correspondence persons for group members
        for (final EWSSyncTaskConfiguration.Selector selector : group.getCorrespondenceGroups()) {
            for (final ProcuratGroupMembership membership : client.getGroupMemberships(selector.getId())) {
                for (final ProcuratCommunication communication : client.getCommunicationsByPersonId(membership.getPersonId())) {
                    selectors.add(new EWSSyncTaskConfiguration.Selector(selector, communication.getContactPersonId()));
                }
            }
        }

        // Specific persons
        selectors.addAll(group.getPersons());
        return selectors;
    }

    private String normalizePhoneNumber(final String phoneNumber) {
        return phoneNumber.replaceAll("[^\\d.]+", "");
    }
}
