package de.waldorfaugsburg.psync.task;

import de.waldorfaugsburg.psync.ProcuratSyncApplication;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

@Slf4j
public class SyncTaskScheduler {

    private final ProcuratSyncApplication application;

    private final Timer timer = new Timer();
    private final List<AbstractSyncTask<?>> tasks = new ArrayList<>();

    public SyncTaskScheduler(final ProcuratSyncApplication application) {
        this.application = application;
        setup();
    }

    public void stopTasks() {
        timer.cancel();
    }

    private void setup() {
        for (final AbstractSyncTaskConfiguration configuration : application.getConfiguration().getTasks()) {
            try {
                final Class<?> taskClass = configuration.getTaskClass();
                if (!AbstractSyncTask.class.isAssignableFrom(taskClass)) {
                    log.error("Invalid task type {}", taskClass.getName());
                    continue;
                }

                final Constructor<?> constructor = taskClass.getDeclaredConstructor(ProcuratSyncApplication.class, configuration.getClass());
                final AbstractSyncTask<?> task = (AbstractSyncTask<?>) constructor.newInstance(application, configuration);
                tasks.add(task);

                log.info("Registered task {} (next run: {})", taskClass.getSimpleName(), task.getNextRun());
            } catch (final NoSuchMethodException | InstantiationException | InvocationTargetException |
                           IllegalAccessException e) {
                log.error("Error instantiating task {}", configuration.getType(), e);
            }
        }

        timer.schedule((new TimerTask() {
            @Override
            public void run() {
                final ZonedDateTime now = ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS);

                for (final AbstractSyncTask<?> task : tasks) {
                    final ZonedDateTime runTime = task.getNextRun().truncatedTo(ChronoUnit.SECONDS);
                    if (runTime.isEqual(now)) {
                        task.runTask();
                    }
                }
            }
        }), 0, 1000);

        for (final AbstractSyncTask<?> task : tasks) {
            if (!task.isRunAtStartup()) continue;

            task.runTask();
        }
    }
}
