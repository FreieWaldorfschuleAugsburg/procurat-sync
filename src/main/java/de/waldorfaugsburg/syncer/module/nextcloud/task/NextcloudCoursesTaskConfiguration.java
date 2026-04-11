package de.waldorfaugsburg.syncer.module.nextcloud.task;

import de.waldorfaugsburg.syncer.task.ScheduledTaskConfiguration;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class NextcloudCoursesTaskConfiguration extends ScheduledTaskConfiguration {

    private String schoolYear;
    private List<NextcloudCourseConfiguration> courses;

}
