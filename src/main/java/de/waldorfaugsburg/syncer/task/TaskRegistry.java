package de.waldorfaugsburg.syncer.task;

import de.waldorfaugsburg.syncer.SyncerApplication;
import de.waldorfaugsburg.syncer.module.ews.task.EWSAddressBookTask;
import de.waldorfaugsburg.syncer.module.starface.task.StarfaceContactsTask;
import lombok.extern.slf4j.Slf4j;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Slf4j
public class TaskRegistry {

    private final SyncerApplication application;
    private final Set<AbstractTask> tasks = new HashSet<>();
    private final Timer timer = new Timer();

    public TaskRegistry(final SyncerApplication application) {
        this.application = application;

        registerTasks();
        loadConfigurations();
        startScheduler();
    }

    private void registerTasks() {
        tasks.add(new StarfaceContactsTask(application));
        tasks.add(new EWSAddressBookTask(application));
    }

    private void loadConfigurations() {
        final Iterator<AbstractTask> iterator = tasks.iterator();
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

                for (final AbstractTask task : tasks) {
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
