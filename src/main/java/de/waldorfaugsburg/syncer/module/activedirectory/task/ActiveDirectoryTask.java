package de.waldorfaugsburg.syncer.module.activedirectory.task;

import de.waldorfaugsburg.syncer.SyncerApplication;
import de.waldorfaugsburg.syncer.module.activedirectory.ActiveDirectoryModule;
import de.waldorfaugsburg.syncer.module.procurat.ProcuratModule;
import de.waldorfaugsburg.syncer.module.procurat.model.ProcuratCommunication;
import de.waldorfaugsburg.syncer.module.procurat.model.ProcuratGroupMembership;
import de.waldorfaugsburg.syncer.task.AbstractScheduledTask;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.Set;

@Slf4j
public class ActiveDirectoryTask extends AbstractScheduledTask {

    private ProcuratModule procuratModule;
    private ActiveDirectoryModule activeDirectoryModule;

    public ActiveDirectoryTask(final SyncerApplication application) {
        super(application, "ad_task.json", ProcuratModule.class, ActiveDirectoryModule.class);
    }

    @Override
    public void run() throws Exception {
        procuratModule = getApplication().getModuleRegistry().getOrCreateInstance(ProcuratModule.class);
        activeDirectoryModule = getApplication().getModuleRegistry().getOrCreateInstance(ActiveDirectoryModule.class);

        final ActiveDirectoryTaskConfiguration taskConfiguration = (ActiveDirectoryTaskConfiguration) getConfiguration();
        for (final ActiveDirectoryMapper mapper : taskConfiguration.getMappers()) {
            try {
                handleMapper(mapper);
            } catch (final Exception e) {
                log.error("Error while handling mapper (name: {})", mapper.getName(), e);
            }
        }
    }

    private void handleMapper(final ActiveDirectoryMapper mapper) throws Exception {
        final Set<Integer> personIds = new HashSet<>();

        for (final int groupId : mapper.getGroups()) {
            for (final ProcuratGroupMembership membership : procuratModule.getGroupMemberships(groupId)) {
                personIds.add(membership.getPersonId());
            }
        }

        for (final int groupId : mapper.getCorrespondenceGroups()) {
            for (final ProcuratGroupMembership membership : procuratModule.getGroupMemberships(groupId)) {
                for (final ProcuratCommunication communication : procuratModule.getCommunicationsByPersonId(membership.getPersonId())) {
                    personIds.add(communication.getContactPersonId());
                }
            }
        }

        personIds.addAll(mapper.getPersons());

        for (final int personId : personIds) {
            try {
                handlePerson(mapper, personId);
            } catch (final Exception e) {
                log.error("Error while handling person (mapper: {}, personId: {})", mapper.getName(), personId, e);
            }
        }
    }

    private void handlePerson(final ActiveDirectoryMapper mapper, int personId) {
        // TODO create a multimap to compare mapper priorities and only apply values from mapper with highest priority
    }
}
