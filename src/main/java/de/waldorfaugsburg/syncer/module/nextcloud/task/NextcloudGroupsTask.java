package de.waldorfaugsburg.syncer.module.nextcloud.task;

import de.waldorfaugsburg.syncer.SyncerApplication;
import de.waldorfaugsburg.syncer.module.nextcloud.NextcloudModule;
import de.waldorfaugsburg.syncer.module.procurat.ProcuratModule;
import de.waldorfaugsburg.syncer.task.AbstractScheduledTask;

public class NextcloudGroupsTask extends AbstractScheduledTask {

    public NextcloudGroupsTask(final SyncerApplication application) {
        super(application, "nextcloud_groups.json", ProcuratModule.class, NextcloudModule.class);
    }

    @Override
    public void run() throws Exception {
        final NextcloudModule nextcloudModule = getApplication().getModuleRegistry().getInstance(NextcloudModule.class);
        nextcloudModule.getAllGroups().forEach(System.out::println);


        nextcloudModule.getGroupMembers("RolleVT").forEach(System.out::println);
    }
}
