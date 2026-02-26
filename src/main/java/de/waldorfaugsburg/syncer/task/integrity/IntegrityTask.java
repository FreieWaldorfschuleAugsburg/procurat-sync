package de.waldorfaugsburg.syncer.task.integrity;

import de.waldorfaugsburg.syncer.SyncerApplication;
import de.waldorfaugsburg.syncer.client.procurat.ProcuratClient;
import de.waldorfaugsburg.syncer.module.procurat.model.ProcuratContactInformation;
import de.waldorfaugsburg.syncer.module.procurat.model.ProcuratPerson;
import de.waldorfaugsburg.syncer.task.AbstractSyncTask;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public final class IntegrityTask extends AbstractSyncTask<IntegrityTaskConfiguration> {

    public IntegrityTask(final SyncerApplication application, final IntegrityTaskConfiguration configuration) {
        super(application, configuration);
    }

    @Override
    public void run() throws Exception {
        final ProcuratClient procuratClient = ProcuratClient.createInstance(getApplication());

        final List<ProcuratPerson> allPersons = procuratClient.getAllPersons();
        for (final ProcuratPerson person : allPersons) {
            if (person.getFirstName() == null || person.getLastName() == null) {
                log.debug("Name is null (personId: '{}')", person.getId());
                continue;
            }

            boolean personChanged = false;

            log.debug("Starting name check (personId: {})", person.getId());

            // Name checks
            final String trimmedFirstName = person.getFirstName().trim();
            if (!trimmedFirstName.equals(person.getFirstName())) {
                recordDeviation("Whitespaces found in 'firstName' (personId: %s, name: %s)", person.getId(), person.getFullName());
                person.setFirstName(trimmedFirstName);
                personChanged = true;
            }

            final String trimmedLastName = person.getLastName().trim();
            if (!trimmedLastName.equals(person.getLastName())) {
                recordDeviation("Whitespaces found in 'lastName' (personId: %s, name: %s)", person.getId(), person.getFullName());
                person.setLastName(trimmedLastName);
                personChanged = true;
            }

            log.debug("Starting contact information check (personId: {})", person.getId());

            // Contact information checks
            final List<ProcuratContactInformation> contactInformation = procuratClient.getContactInformationByPersonId(person.getId());
            for (final ProcuratContactInformation info : contactInformation) {
                boolean infoChanged = false;

                if (info.getMedium().equals("email")) {
                    final String trimmedEmail = info.getContent().trim();
                    if (!trimmedEmail.equals(info.getContent())) {
                        recordDeviation("Whitespaces found in 'email' (personId: %s, name: %s, email: %s)", person.getId(), person.getFullName(), info.getContent());
                        info.setContent(trimmedEmail);
                        infoChanged = true;
                    }

                    if (!trimmedEmail.matches("^[\\w-.]+@([\\w-]+\\.)+[\\w-]{2,4}$")) {
                        // Manual intervention necessary
                        recordDeviation("Regex mismatch in 'email' (personId: %s, name: %s, email: %s)", person.getId(), person.getFullName(), trimmedEmail);
                    }
                }

                if (infoChanged) {
                    log.info("Update contact information (personId: {}, medium: {}, type: {})", person.getId(), info.getMedium(), info.getType());
                }
            }

            if (personChanged) {
                log.info("Update person (personId: {})", person.getId());
            }
        }
    }
}
