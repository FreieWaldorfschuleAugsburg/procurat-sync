package de.waldorfaugsburg.syncer.task;

import com.cronutils.model.Cron;
import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinition;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.time.ExecutionTime;
import com.cronutils.parser.CronParser;
import com.google.common.base.Preconditions;
import de.waldorfaugsburg.syncer.SyncerApplication;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@ToString
public abstract class AbstractSyncTask<T extends AbstractSyncTaskConfiguration> {

    private static final CronDefinition CRON_DEFINITION = CronDefinitionBuilder.instanceDefinitionFor(CronType.UNIX);
    private static final CronParser CRON_PARSER = new CronParser(CRON_DEFINITION);
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

    private final SyncerApplication application;
    private final T configuration;
    private final Map<ZonedDateTime, String> deviationMap = new HashMap<>();

    @Getter
    private final boolean runAtStartup;

    @Getter
    private ZonedDateTime nextRun;

    @Getter
    private boolean running = false;

    protected AbstractSyncTask(final SyncerApplication application, final T configuration) {
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

            // Mail exception
            application.getMailer().sendMail("exception", "%task%", getClass().getSimpleName(), "%error%", e.getMessage());
        } finally {
            running = false;
            log.info("Finished task {} within {}ms (next run: {})", getClass().getSimpleName(), System.currentTimeMillis() - startMillis, getNextRun());

            if (!deviationMap.isEmpty()) {
                log.warn("Recorded {} deviations for task {}", deviationMap.size(), getClass().getSimpleName());

                int count = 1;
                final StringBuilder deviationHtmlList = new StringBuilder();
                for (final Map.Entry<ZonedDateTime, String> entry : deviationMap.entrySet()) {
                    final String formattedMessage = String.format("#%s (%s): %s", count, entry.getKey().format(DATE_TIME_FORMATTER), entry.getValue());
                    deviationHtmlList.append("<li>").append(formattedMessage).append("</li>");
                    log.warn(formattedMessage);
                    count++;
                }
                deviationMap.clear();

                // Mail deviation list
                application.getMailer().sendMail("deviations", "%task%", getClass().getSimpleName(), "%deviations%", deviationHtmlList.toString());
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

    protected SyncerApplication getApplication() {
        return application;
    }

    protected T getConfiguration() {
        return configuration;
    }
}
