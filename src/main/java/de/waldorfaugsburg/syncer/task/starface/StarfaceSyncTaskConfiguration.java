package de.waldorfaugsburg.syncer.task.starface;

import de.waldorfaugsburg.syncer.task.AbstractSyncTaskConfiguration;

public final class StarfaceSyncTaskConfiguration extends AbstractSyncTaskConfiguration {

    @Override
    public Class<?> getTaskClass() {
        return StarfaceSyncTask.class;
    }
    
}
