package de.waldorfaugsburg.psync.task.mensamax;

import com.google.gson.JsonObject;
import de.waldorfaugsburg.psync.ProcuratSyncApplication;
import de.waldorfaugsburg.psync.task.AbstractSyncTask;

public class MensaMaxSyncTask extends AbstractSyncTask {

    protected MensaMaxSyncTask(ProcuratSyncApplication application, JsonObject customData) {
        super(application, customData);
    }

    @Override
    public void sync() {

    }
}
