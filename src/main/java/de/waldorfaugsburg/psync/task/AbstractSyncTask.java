package de.waldorfaugsburg.psync.task;

import com.google.gson.JsonObject;
import de.waldorfaugsburg.psync.ProcuratSyncApplication;

public abstract class AbstractSyncTask {

    private final ProcuratSyncApplication application;
    private final JsonObject customData;

    protected AbstractSyncTask(final ProcuratSyncApplication application, final JsonObject customData) {
        this.application = application;
        this.customData = customData;
    }

    public abstract void sync();

    protected ProcuratSyncApplication getApplication() {
        return application;
    }

    protected JsonObject getCustomData() {
        return customData;
    }
}
