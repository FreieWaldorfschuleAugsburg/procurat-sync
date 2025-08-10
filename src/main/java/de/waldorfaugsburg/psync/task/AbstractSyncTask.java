package de.waldorfaugsburg.psync.task;

import com.cronutils.model.Cron;
import com.cronutils.model.time.ExecutionTime;
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

    protected AbstractSyncTask(final ProcuratSyncApplication application, final Cron interval, final JsonObject customData) {
        this.application = application;
        this.interval = interval;
        this.customData = customData;
        updateNextRun();
    }

    public abstract void run();

    public void runTask() {
        log.info("Starting task {}", getClass().getSimpleName());
        updateNextRun();
        run();
        log.info("Finished task {} (next run: {})", getClass().getSimpleName(), getNextRun());
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
