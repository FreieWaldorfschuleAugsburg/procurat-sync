package de.waldorfaugsburg.psync.task.starface;

import de.waldorfaugsburg.psync.task.AbstractSyncTaskConfiguration;

public class StarfaceSyncTaskConfiguration extends AbstractSyncTaskConfiguration {

    @Override
    public Class<?> getTaskClass() {
        return StarfaceSyncTask.class;
    }
    
}
