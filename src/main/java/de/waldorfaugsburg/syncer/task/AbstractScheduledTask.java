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
public abstract class AbstractScheduledTask<C extends ScheduledTaskConfiguration> extends AbstractTask<C> {

    private static final CronDefinition CRON_DEFINITION = CronDefinitionBuilder.instanceDefinitionFor(CronType.UNIX);
    private static final CronParser CRON_PARSER = new CronParser(CRON_DEFINITION);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Getter
    private ZonedDateTime nextRun;

    @SafeVarargs
    public AbstractScheduledTask(final SyncerApplication application,
                                 final String configurationPath,
                                 final Class<C> configurationClass,
                                 final Class<? extends AbstractModule>... modules) {
        super(application, configurationPath, configurationClass, modules);
    }

    @Override
    public void loadConfiguration() throws Exception {
        super.loadConfiguration();

        final Cron interval = CRON_PARSER.parse(getConfiguration().getCron());
        nextRun = ExecutionTime.forCron(interval).nextExecution(ZonedDateTime.now()).orElseThrow();
        log.info("Task {} scheduled for {}", this.getClass().getSimpleName(), nextRun.format(FORMATTER));
    }
}
