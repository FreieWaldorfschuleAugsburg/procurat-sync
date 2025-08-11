package de.waldorfaugsburg.psync.task;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public abstract class AbstractSyncTaskConfiguration {
    private String type;
    private String interval;
    private boolean runAtStartup;

    public abstract Class<?> getTaskClass();
}
