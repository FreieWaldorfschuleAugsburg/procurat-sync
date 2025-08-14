package de.waldorfaugsburg.psync.task;

import com.cronutils.model.Cron;
import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinition;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.time.ExecutionTime;
import com.cronutils.parser.CronParser;
import com.google.common.base.Preconditions;
import com.google.gson.JsonObject;
import de.waldorfaugsburg.psync.ProcuratSyncApplication;
import de.waldorfaugsburg.psync.config.ApplicationConfiguration;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@ToString
public abstract class AbstractSyncTask<T extends AbstractSyncTaskConfiguration> {

    private static final CronDefinition CRON_DEFINITION = CronDefinitionBuilder.instanceDefinitionFor(CronType.UNIX);
    private static final CronParser CRON_PARSER = new CronParser(CRON_DEFINITION);
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

    private final ProcuratSyncApplication application;
    private final T configuration;

    @Getter
    private final boolean runAtStartup;

    @Getter
    private ZonedDateTime nextRun;

    @Getter
    private boolean running = false;

    private Map<ZonedDateTime, String> deviationMap = new HashMap<>();

    protected AbstractSyncTask(final ProcuratSyncApplication application, final T configuration) {
        this.application = application;
        this.configuration = configuration;
        this.runAtStartup = configuration.isRunAtStartup();
        updateNextRun();
    }

    public abstract void run() throws Exception;

    public void runTask() {
        Preconditions.checkState(!running, "task already running.");
        log.info("Starting task {}", getClass().getSimpleName());

        updateNextRun();
        running = true;
        final long startMillis = System.currentTimeMillis();

        try {
            run();
        } catch (final Exception e) {
            log.error("Error running task {}", getClass().getSimpleName(), e);
        } finally {
            running = false;
            log.info("Finished task {} within {}ms (next run: {})", getClass().getSimpleName(), System.currentTimeMillis() - startMillis, getNextRun());

            if (!deviationMap.isEmpty()) {
                log.warn("Recorded {} deviations for task {}", deviationMap.size(), getClass().getSimpleName());
                int count = 1;
                for (final Map.Entry<ZonedDateTime, String> entry : deviationMap.entrySet()) {
                    log.warn("#{} ({}): {}", count, entry.getKey().format(DATE_TIME_FORMATTER), entry.getValue());
                    count++;
                }
                deviationMap.clear();
            }
        }
    }

    private void updateNextRun() {
        final Cron interval = CRON_PARSER.parse(configuration.getInterval());
        nextRun = ExecutionTime.forCron(interval).nextExecution(ZonedDateTime.now()).orElseThrow();
    }

    protected void recordDeviation(final String message, final Object... args) {
        final String formattedMessage = String.format(message, args);
        deviationMap.put(ZonedDateTime.now(), formattedMessage);
        log.warn(formattedMessage);
    }

    protected ProcuratSyncApplication getApplication() {
        return application;
    }

    protected T getConfiguration() {
        return configuration;
    }

}
