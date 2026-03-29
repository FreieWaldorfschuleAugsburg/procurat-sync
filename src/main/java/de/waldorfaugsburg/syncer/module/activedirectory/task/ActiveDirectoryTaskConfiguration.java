package de.waldorfaugsburg.syncer.module.activedirectory.task;

import de.waldorfaugsburg.syncer.task.ScheduledTaskConfiguration;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class ActiveDirectoryTaskConfiguration extends ScheduledTaskConfiguration {

    private List<ActiveDirectoryMapper> mappers;

}
