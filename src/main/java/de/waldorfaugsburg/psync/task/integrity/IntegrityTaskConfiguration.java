package de.waldorfaugsburg.psync.task.integrity;

import de.waldorfaugsburg.psync.task.AbstractSyncTaskConfiguration;

public final class IntegrityTaskConfiguration extends AbstractSyncTaskConfiguration {

    @Override
    public Class<?> getTaskClass() {
        return IntegrityTask.class;
    }
    
}
