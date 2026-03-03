package de.waldorfaugsburg.syncer.module.ews.task;

import de.waldorfaugsburg.syncer.SyncerApplication;
import de.waldorfaugsburg.syncer.module.ews.EWSModule;
import de.waldorfaugsburg.syncer.task.AbstractScheduledTask;
import de.waldorfaugsburg.syncer.task.ScheduledTaskConfiguration;

public class EWSAddressBookTask extends AbstractScheduledTask {

    public EWSAddressBookTask(final SyncerApplication application) {
        super(application, "ews_address_book.json", EWSAddressBookTask.Config.class, EWSModule.class);
    }

    @Override
    public void run() throws Exception {

    }

    public static class Config extends ScheduledTaskConfiguration {

    }
}
