package de.waldorfaugsburg.syncer.task;

import de.waldorfaugsburg.syncer.SyncerApplication;
import de.waldorfaugsburg.syncer.module.starface.task.StarfaceContactsTask;
import lombok.extern.slf4j.Slf4j;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

@Slf4j
public class TaskRegistry {

    private final SyncerApplication application;
    private final Map<Class<? extends AbstractTask<?>>, AbstractTask<?>> taskMap = new HashMap<>();
    private final Timer timer = new Timer();

    public TaskRegistry(final SyncerApplication application) {
        this.application = application;

        registerTasks();
    }

    private void registerTasks() {
        taskMap.put(StarfaceContactsTask.class, new StarfaceContactsTask(application));
    }

    private void startScheduler() {
        for (final AbstractTask<?> task : taskMap.values()) {
            try {
                task.loadConfiguration();
            } catch (final Exception e) {
                taskMap.remove(task.getClass());
                log.error("Error while loading task configuration. Task {} disabled", task.getClass().getSimpleName(), e);
            }
        }

        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                final ZonedDateTime now = ZonedDateTime.now();

                for (final AbstractTask<?> task : taskMap.values()) {
                    if (task instanceof AbstractScheduledTask<?> scheduledTask) {
                        if (scheduledTask.getNextRun().truncatedTo(ChronoUnit.SECONDS).isEqual(now)) {
                            task.invoke();
                        }
                    }
                }
            }
        }, 0, 1000);
    }
}
