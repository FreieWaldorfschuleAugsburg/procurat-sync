package de.waldorfaugsburg.syncer.module.nextcloud.task;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Getter
@NoArgsConstructor
public class NextcloudCourseConfiguration {

    private String name;
    private List<Integer> groupIds;
    private List<String> teachers;
    private Map<String, String> studentFilter;

}
