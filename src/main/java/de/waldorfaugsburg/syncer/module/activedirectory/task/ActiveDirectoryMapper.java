package de.waldorfaugsburg.syncer.module.activedirectory.task;

import de.waldorfaugsburg.syncer.task.ScheduledTaskConfiguration;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Set;

@Getter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, of = {"name"})
public class ActiveDirectoryMapper extends ScheduledTaskConfiguration {

    private String name;
    private Set<Integer> groups;
    private Set<Integer> correspondenceGroups;
    private Set<Integer> persons;
    private String targetDN;
    private Set<String> targetGroups;
    private String upnStrategy;
    private String title;
    private String office;
    private String description;

}
