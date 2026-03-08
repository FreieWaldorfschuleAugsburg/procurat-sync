package de.waldorfaugsburg.syncer.module.starface.task;

import de.waldorfaugsburg.syncer.SyncerApplication;
import de.waldorfaugsburg.syncer.module.procurat.ProcuratModule;
import de.waldorfaugsburg.syncer.module.procurat.model.ProcuratContactInformation;
import de.waldorfaugsburg.syncer.module.procurat.model.ProcuratGroupMembership;
import de.waldorfaugsburg.syncer.module.procurat.model.ProcuratPerson;
import de.waldorfaugsburg.syncer.module.starface.StarfaceModule;
import de.waldorfaugsburg.syncer.task.AbstractScheduledTask;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
public class StarfaceContactsTask extends AbstractScheduledTask {

    public StarfaceContactsTask(final SyncerApplication application) {
        super(application, "starface_contacts.json", ProcuratModule.class, StarfaceModule.class);
    }

    @Override
    public void run() throws Exception {
        final ProcuratModule procuratModule = getApplication().getModuleRegistry().getOrCreateInstance(ProcuratModule.class);
        final StarfaceModule starfaceModule = getApplication().getModuleRegistry().getOrCreateInstance(StarfaceModule.class);

        // Delete all contacts to start fresh
        int deletedCount = starfaceModule.deleteAllContacts();
        log.info("Deleted {} contacts", deletedCount);

        final Map<Integer, ProcuratPerson> personMap = procuratModule.getAllPersonsMap();
        final List<ProcuratGroupMembership> rootGroupMemberships = procuratModule.getRootGroupMemberships();

        int count = 0;
        for (final ProcuratGroupMembership membership : rootGroupMemberships) {
            try {
                final ProcuratPerson person = personMap.get(membership.getPersonId());

                final List<ProcuratContactInformation> addressContactInfo = procuratModule.getContactInformationByAddressId(person.getAddressId());
                final List<ProcuratContactInformation> personContactInfo = procuratModule.getContactInformationByPersonId(person.getId());

                String homePhone = null;
                for (final ProcuratContactInformation addressInfo : addressContactInfo) {
                    // Only accept landline numbers
                    if (!addressInfo.getMedium().equals("telephone")) continue;

                    homePhone = normalizePhoneNumber(addressInfo.getContent());
                    break;
                }

                final List<String> phoneNumbers = new ArrayList<>();
                for (final ProcuratContactInformation personInfo : personContactInfo) {
                    if (!personInfo.getMedium().equals("telephone") && !personInfo.getMedium().equals("mobile"))
                        continue;
                    // Skip phone numbers flagged as secret
                    if (personInfo.isSecret()) continue;

                    phoneNumbers.add(normalizePhoneNumber(personInfo.getContent()));
                }

                // Skip person without any personal phone numbers (e.g. students)
                if (phoneNumbers.isEmpty()) {
                    log.debug("Skipping person without phone numbers (id: {})", person.getId());
                    continue;
                }

                // Remove possible duplicate home phone number
                phoneNumbers.remove(homePhone);

                log.debug("Create contact (id: {}, name: {}, homePhone: {}, numbers: {})", person.getId(), person.getFullName(), homePhone, String.join(";", phoneNumbers));
                starfaceModule.createContact(person.getFirstName(), person.getLastName(), homePhone, phoneNumbers);

                count++;
            } catch (final Exception e) {
                log.error("Error creating contact (id: {})", membership.getPersonId());
            }
        }

        log.info("Created {} contacts from {} persons", count, rootGroupMemberships.size());
    }

    private String normalizePhoneNumber(final String phoneNumber) {
        return phoneNumber.replaceAll("[^\\d.]+", "");
    }
}
