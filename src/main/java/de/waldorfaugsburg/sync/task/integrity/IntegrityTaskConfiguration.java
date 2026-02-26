package de.waldorfaugsburg.sync.task.integrity;

import de.waldorfaugsburg.sync.task.AbstractSyncTaskConfiguration;

public final class IntegrityTaskConfiguration extends AbstractSyncTaskConfiguration {

    @Override
    public Class<?> getTaskClass() {
        return IntegrityTask.class;
    }
    
}
