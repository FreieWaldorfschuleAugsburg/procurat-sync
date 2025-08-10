package de.waldorfaugsburg.psync.task.ews;

import com.cronutils.model.Cron;
import com.google.gson.JsonObject;
import de.waldorfaugsburg.psync.ProcuratSyncApplication;
import de.waldorfaugsburg.psync.client.ews.EWSClient;
import de.waldorfaugsburg.psync.client.procurat.ProcuratClient;
import de.waldorfaugsburg.psync.client.procurat.model.ProcuratAddress;
import de.waldorfaugsburg.psync.client.procurat.model.ProcuratContactInformation;
import de.waldorfaugsburg.psync.client.procurat.model.ProcuratGroupMembership;
import de.waldorfaugsburg.psync.client.procurat.model.ProcuratPerson;
import de.waldorfaugsburg.psync.task.AbstractSyncTask;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class EWSSyncTask extends AbstractSyncTask {

    public EWSSyncTask(final ProcuratSyncApplication application, final Cron interval, final JsonObject customData) {
        super(application, interval, customData);
    }

    @Override
    public void run() {
        final EWSClient client = getApplication().getEwsClient();

        // Delete all contacts to start fresh
        if (!client.deleteAllContacts()) {
            return;
        }

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
            client.addContact(person.getFirstName(), person.getLastName(), email, homePhone, mobilePhone, address.getCity(), address.getZip(), address.getStreet(), "");
            count++;

            if (count == 20) {
                return;
            }
        }
    }

    private String normalizePhoneNumber(final String phoneNumber) {
        return phoneNumber.replaceAll("[^\\d.]+", "");
    }
}
