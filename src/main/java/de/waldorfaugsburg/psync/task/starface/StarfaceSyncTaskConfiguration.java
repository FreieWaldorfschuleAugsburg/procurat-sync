package de.waldorfaugsburg.psync.task.starface;

import de.waldorfaugsburg.psync.task.AbstractSyncTaskConfiguration;

public final class StarfaceSyncTaskConfiguration extends AbstractSyncTaskConfiguration {

    @Override
    public Class<?> getTaskClass() {
        return StarfaceSyncTask.class;
    }
    
}
