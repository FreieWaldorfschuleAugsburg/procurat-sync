package de.waldorfaugsburg.syncer.module.starface.task;

import de.waldorfaugsburg.syncer.SyncerApplication;
import de.waldorfaugsburg.syncer.module.procurat.ProcuratModule;
import de.waldorfaugsburg.syncer.module.starface.StarfaceModule;
import de.waldorfaugsburg.syncer.task.AbstractScheduledTask;
import de.waldorfaugsburg.syncer.task.ScheduledTaskConfiguration;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class StarfaceContactsTask extends AbstractScheduledTask<StarfaceContactsTask.Config> {

    public StarfaceContactsTask(final SyncerApplication application) {
        super(application, "starface_contacts.json",
                StarfaceContactsTask.Config.class,
                ProcuratModule.class, StarfaceModule.class);
    }

    @Override
    public void run() throws Exception {
        final ProcuratModule procuratModule = getApplication().getModuleRegistry().getOrCreateInstance(ProcuratModule.class);
        final StarfaceModule starfaceModule = getApplication().getModuleRegistry().getOrCreateInstance(StarfaceModule.class);


    }

    @NoArgsConstructor
    @Getter
    public static class Config extends ScheduledTaskConfiguration {

    }
}
