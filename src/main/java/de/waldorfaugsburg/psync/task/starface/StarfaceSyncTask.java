package de.waldorfaugsburg.psync.task.starface;

import com.google.gson.JsonObject;
import de.waldorfaugsburg.psync.ProcuratSyncApplication;
import de.waldorfaugsburg.psync.client.procurat.ProcuratClient;
import de.waldorfaugsburg.psync.client.procurat.model.ProcuratContactInformation;
import de.waldorfaugsburg.psync.client.procurat.model.ProcuratGroupMembership;
import de.waldorfaugsburg.psync.client.procurat.model.ProcuratPerson;
import de.waldorfaugsburg.psync.task.AbstractSyncTask;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class StarfaceSyncTask extends AbstractSyncTask {

    public StarfaceSyncTask(final ProcuratSyncApplication application, final JsonObject customData) {
        super(application, customData);
    }

    @Override
    public void sync() {
        final ProcuratClient procuratClient = getApplication().getProcuratClient();

        final List<ProcuratPerson> persons = procuratClient.execute(procuratClient.getPersonService().findAll());
        final List<ProcuratGroupMembership> rootMemberships = procuratClient.getRootGroupMemberships();

        getApplication().getStarfaceClient().deleteAllContacts();
        int count = 0;
        for (final ProcuratPerson person : persons) {
            if (!procuratClient.isPersonActiveMember(rootMemberships, person)) {
                log.info("Skipping inactive person {}", person);
                continue;
            }

            final List<ProcuratContactInformation> addressContactInfo = procuratClient.execute(procuratClient.getContactInformationService().findByAddressId(person.getAddressId()));
            final List<ProcuratContactInformation> personContactInfo = procuratClient.execute(procuratClient.getContactInformationService().findByPersonId(person.getId()));

            String homePhone = null;
            for (final ProcuratContactInformation addressInfo : addressContactInfo) {
                if (!addressInfo.getMedium().equals("telephone")) continue;

                homePhone = normalizePhoneNumber(addressInfo.getContent());
                break;
            }

            final List<String> phoneNumbers = new ArrayList<>();

            for (final ProcuratContactInformation personInfo : personContactInfo) {
                if (!personInfo.getMedium().equals("telephone") && !personInfo.getMedium().equals("mobile")) continue;
                if (personInfo.isSecret()) continue;

                final String normalizedPhoneNumber = normalizePhoneNumber(personInfo.getContent());
                if (normalizedPhoneNumber.equals(homePhone)) {
                    log.info("Ignoring duplicate home phone {} for person {}", homePhone, person);
                    homePhone = null;
                }

                phoneNumbers.add(normalizedPhoneNumber);
            }

            if (phoneNumbers.isEmpty()) {
                log.info("Skipping person {} because there are no phone numbers", person);
                continue;
            }

            getApplication().getStarfaceClient().createContact(person.getFirstName(), person.getLastName(), homePhone, phoneNumbers);
            count++;
        }

        log.info("Created {} contacts from {} persons", count, persons.size());
    }

    private String normalizePhoneNumber(final String phoneNumber) {
        return phoneNumber.replaceAll("[^\\d.]+", "");
    }
}
