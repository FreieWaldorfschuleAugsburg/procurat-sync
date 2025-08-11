package de.waldorfaugsburg.psync.task;

import com.cronutils.model.Cron;
import com.cronutils.model.time.ExecutionTime;
import com.google.common.base.Preconditions;
import com.google.gson.JsonObject;
import de.waldorfaugsburg.psync.ProcuratSyncApplication;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.time.ZonedDateTime;

@Slf4j
@ToString
public abstract class AbstractSyncTask {

    private final ProcuratSyncApplication application;
    private final Cron interval;
    private final JsonObject customData;

    @Getter
    private ZonedDateTime nextRun;

    @Getter
    private boolean running = false;

    protected AbstractSyncTask(final ProcuratSyncApplication application, final Cron interval, final JsonObject customData) {
        this.application = application;
        this.interval = interval;
        this.customData = customData;
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
        }
    }

    private void updateNextRun() {
        nextRun = ExecutionTime.forCron(interval).nextExecution(ZonedDateTime.now()).orElseThrow();
    }

    protected ProcuratSyncApplication getApplication() {
        return application;
    }

    protected Cron getInterval() {
        return interval;
    }

    protected JsonObject getCustomData() {
        return customData;
    }

}
