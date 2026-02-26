package de.waldorfaugsburg.syncer.task.integrity;

import de.waldorfaugsburg.syncer.task.AbstractSyncTaskConfiguration;

public final class IntegrityTaskConfiguration extends AbstractSyncTaskConfiguration {

    @Override
    public Class<?> getTaskClass() {
        return IntegrityTask.class;
    }
    
}
