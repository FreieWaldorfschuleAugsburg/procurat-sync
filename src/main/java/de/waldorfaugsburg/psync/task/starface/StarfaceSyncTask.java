package de.waldorfaugsburg.psync.task.starface;

import de.waldorfaugsburg.psync.ProcuratSyncApplication;
import de.waldorfaugsburg.psync.client.procurat.ProcuratClient;
import de.waldorfaugsburg.psync.client.procurat.model.ProcuratContactInformation;
import de.waldorfaugsburg.psync.client.procurat.model.ProcuratGroupMembership;
import de.waldorfaugsburg.psync.client.procurat.model.ProcuratPerson;
import de.waldorfaugsburg.psync.client.starface.StarfaceClient;
import de.waldorfaugsburg.psync.task.AbstractSyncTask;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class StarfaceSyncTask extends AbstractSyncTask<StarfaceSyncTaskConfiguration> {

    public StarfaceSyncTask(final ProcuratSyncApplication application, final StarfaceSyncTaskConfiguration configuration) {
        super(application, configuration);
    }

    @Override
    public void run() throws Exception {
        final StarfaceClient starfaceClient = getApplication().getStarfaceClient();

        // Delete all contacts to start fresh
        starfaceClient.deleteAllContacts();

        final ProcuratClient procuratClient = getApplication().getProcuratClient();
        final List<ProcuratPerson> persons = procuratClient.getAllPersons();
        final List<ProcuratGroupMembership> rootMemberships = procuratClient.getRootGroupMemberships();

        int count = 0;
        for (final ProcuratPerson person : persons) {
            // Check if person is an active member of the root group
            if (!procuratClient.isPersonActiveMember(rootMemberships, person)) {
                log.info("Skipping inactive person {}", person);
                continue;
            }

            final List<ProcuratContactInformation> addressContactInfo = procuratClient.getContactInformationByAddressId(person.getAddressId());
            final List<ProcuratContactInformation> personContactInfo = procuratClient.getContactInformationByPersonId(person.getId());

            String homePhone = null;
            for (final ProcuratContactInformation addressInfo : addressContactInfo) {
                // Only accept landline numbers
                if (!addressInfo.getMedium().equals("telephone")) continue;

                homePhone = normalizePhoneNumber(addressInfo.getContent());
                break;
            }

            final List<String> phoneNumbers = new ArrayList<>();
            for (final ProcuratContactInformation personInfo : personContactInfo) {
                if (!personInfo.getMedium().equals("telephone") && !personInfo.getMedium().equals("mobile")) continue;
                // Skip phone numbers flagged as secret
                if (personInfo.isSecret()) continue;

                phoneNumbers.add(normalizePhoneNumber(personInfo.getContent()));
            }

            // Skip person without any personal phone numbers (e.g. students)
            if (phoneNumbers.isEmpty()) {
                log.info("Skipping person {} because there are no phone numbers", person);
                continue;
            }

            // Remove possible duplicate home phone number
            phoneNumbers.remove(homePhone);

            starfaceClient.createContact(person.getFirstName(), person.getLastName(), homePhone, phoneNumbers);
            count++;
        }

        log.info("Created {} contacts from {} persons", count, persons.size());
    }

    private String normalizePhoneNumber(final String phoneNumber) {
        return phoneNumber.replaceAll("[^\\d.]+", "");
    }
}
