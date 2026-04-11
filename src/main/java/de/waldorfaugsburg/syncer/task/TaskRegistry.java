package de.waldorfaugsburg.syncer.task;

import de.waldorfaugsburg.syncer.SyncerApplication;
import de.waldorfaugsburg.syncer.module.activedirectory.task.ActiveDirectoryTask;
import de.waldorfaugsburg.syncer.module.ews.task.addressbook.EWSAddressBookTask;
import de.waldorfaugsburg.syncer.module.nextcloud.task.NextcloudCoursesTask;
import de.waldorfaugsburg.syncer.module.starface.task.StarfaceContactsTask;
import lombok.extern.slf4j.Slf4j;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Slf4j
public class TaskRegistry {

    private final SyncerApplication application;
    private final Map<String, AbstractTask> taskMap = new HashMap<>();
    private final Timer timer = new Timer();

    public TaskRegistry(final SyncerApplication application) {
        this.application = application;

        registerTasks();
        loadConfigurations();
        startScheduler();
    }

    public void invokeTask(final String taskName) {
        taskMap.get(taskName).invoke();
    }

    private void registerTasks() {
        taskMap.put("starface", new StarfaceContactsTask(application));
        taskMap.put("ews", new EWSAddressBookTask(application));
        taskMap.put("ad", new ActiveDirectoryTask(application));
        taskMap.put("nc_groups", new NextcloudCoursesTask(application));
    }

    private void loadConfigurations() {
        final Iterator<AbstractTask> iterator = taskMap.values().iterator();
        while (iterator.hasNext()) {
            final AbstractTask task = iterator.next();

            try {
                if (task.loadConfiguration()) {
                    log.info("Task {} configuration loaded", task.getClass().getSimpleName());
                }
            } catch (final Exception e) {
                iterator.remove();
                log.error("Error while loading task configuration. Task {} disabled", task.getClass().getSimpleName(), e);
            }
        }
    }

    private void startScheduler() {
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                final ZonedDateTime now = ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS);

                for (final AbstractTask task : taskMap.values()) {
                    if (task instanceof AbstractScheduledTask scheduledTask) {
                        if (scheduledTask.getNextRun().truncatedTo(ChronoUnit.SECONDS).isEqual(now)) {
                            task.invoke();
                        }
                    }
                }

            }
        }, 0, 1000);
    }
}
