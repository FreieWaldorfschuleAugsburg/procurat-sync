package de.waldorfaugsburg.psync.task;

import com.cronutils.model.Cron;
import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinition;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.time.ExecutionTime;
import com.cronutils.parser.CronParser;
import com.google.gson.JsonObject;
import de.waldorfaugsburg.psync.ProcuratSyncApplication;
import de.waldorfaugsburg.psync.config.ApplicationConfiguration;
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
    private final CronDefinition cronDefinition = CronDefinitionBuilder.instanceDefinitionFor(CronType.UNIX);
    private final CronParser parser = new CronParser(cronDefinition);

    private final Timer timer = new Timer();
    private final List<AbstractSyncTask> tasks = new ArrayList<>();

    public SyncTaskScheduler(final ProcuratSyncApplication application) {
        this.application = application;
        setup();
    }

    public void stopTasks() {
        timer.cancel();
    }

    private void setup() {
        for (final ApplicationConfiguration.TaskConfiguration configuration : application.getConfiguration().getTasks()) {
            try {
                final Class<?> taskClass = Class.forName(configuration.getType());
                if (!AbstractSyncTask.class.isAssignableFrom(taskClass)) {
                    log.error("Invalid task type {}", taskClass.getName());
                    continue;
                }

                final Constructor<?> constructor = taskClass.getConstructor(ProcuratSyncApplication.class, Cron.class, JsonObject.class);
                final AbstractSyncTask task = (AbstractSyncTask) constructor.newInstance(application, parser.parse(configuration.getInterval()), configuration.getCustom());
                tasks.add(task);

                log.info("Registered task {} (next run: {})", taskClass.getSimpleName(), task.getNextRun());
            } catch (final ClassNotFoundException e) {
                log.error("Invalid task type {}", configuration.getType(), e);
            } catch (final NoSuchMethodException | InstantiationException | InvocationTargetException |
                           IllegalAccessException e) {
                log.error("Error instantiating task {}", configuration.getType(), e);
            }
        }

        timer.schedule((new TimerTask() {
            @Override
            public void run() {
                final ZonedDateTime now = ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS);

                for (final AbstractSyncTask task : tasks) {
                    final ZonedDateTime runTime = task.getNextRun().truncatedTo(ChronoUnit.SECONDS);
                    if (runTime.isEqual(now)) {
                        task.runTask();
                    }
                }
            }
        }), 0, 1000);
    }
}
