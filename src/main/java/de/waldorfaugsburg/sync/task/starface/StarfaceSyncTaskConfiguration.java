package de.waldorfaugsburg.sync.task.starface;

import de.waldorfaugsburg.sync.task.AbstractSyncTaskConfiguration;

public final class StarfaceSyncTaskConfiguration extends AbstractSyncTaskConfiguration {

    @Override
    public Class<?> getTaskClass() {
        return StarfaceSyncTask.class;
    }
    
}
