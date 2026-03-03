package de.waldorfaugsburg.syncer.task;

import com.cronutils.model.Cron;
import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinition;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.time.ExecutionTime;
import com.cronutils.parser.CronParser;
import de.waldorfaugsburg.syncer.SyncerApplication;
import de.waldorfaugsburg.syncer.module.AbstractModule;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
public abstract class AbstractScheduledTask extends AbstractTask {

    private static final CronDefinition CRON_DEFINITION = CronDefinitionBuilder.instanceDefinitionFor(CronType.UNIX);
    private static final CronParser CRON_PARSER = new CronParser(CRON_DEFINITION);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Getter
    private ZonedDateTime nextRun;

    @SafeVarargs
    public AbstractScheduledTask(final SyncerApplication application, final String configurationPath,
                                 final Class<? extends AbstractModule>... modules) {
        this(application, configurationPath, ScheduledTaskConfiguration.class, modules);
    }

    @SafeVarargs
    public AbstractScheduledTask(final SyncerApplication application,
                                 final String configurationPath,
                                 final Class<? extends ScheduledTaskConfiguration> configurationClass,
                                 final Class<? extends AbstractModule>... modules) {
        super(application, configurationPath, configurationClass, modules);
    }

    @Override
    public boolean loadConfiguration() throws Exception {
        super.loadConfiguration();

        if (getConfiguration() == null) {
            throw new IllegalStateException("configuration is null");
        }

        final ScheduledTaskConfiguration configuration = (ScheduledTaskConfiguration) getConfiguration();
        final Cron cron = CRON_PARSER.parse(configuration.getCron());
        nextRun = ExecutionTime.forCron(cron).nextExecution(ZonedDateTime.now()).orElseThrow();
        log.info("Task {} scheduled to run at {}", this.getClass().getSimpleName(), FORMATTER.format(nextRun));

        return true;
    }

    @Override
    public void postRun() throws Exception {
        super.postRun();
        loadConfiguration();
    }
}
