package de.waldorfaugsburg.syncer.module.nextcloud.task;

import com.google.gson.JsonObject;
import de.waldorfaugsburg.syncer.SyncerApplication;
import de.waldorfaugsburg.syncer.module.nextcloud.NextcloudModule;
import de.waldorfaugsburg.syncer.module.nextcloud.model.NextcloudCourse;
import de.waldorfaugsburg.syncer.module.procurat.ProcuratModule;
import de.waldorfaugsburg.syncer.module.procurat.model.ProcuratGroup;
import de.waldorfaugsburg.syncer.module.procurat.model.ProcuratGroupMembership;
import de.waldorfaugsburg.syncer.task.AbstractScheduledTask;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
public class NextcloudCoursesTask extends AbstractScheduledTask {

    private ProcuratModule procuratModule;
    private NextcloudModule nextcloudModule;

    public NextcloudCoursesTask(final SyncerApplication application) {
        super(application, "nextcloud_courses.json", NextcloudCoursesTaskConfiguration.class, ProcuratModule.class, NextcloudModule.class);
    }

    @Override
    public void run() throws Exception {
        procuratModule = getApplication().getModuleRegistry().getInstance(ProcuratModule.class);
        nextcloudModule = getApplication().getModuleRegistry().getInstance(NextcloudModule.class);

        final NextcloudCoursesTaskConfiguration configuration = (NextcloudCoursesTaskConfiguration) getConfiguration();

        log.info("Loading courses");

        final List<NextcloudCourse> courses = new ArrayList<>();
        for (final NextcloudCourseConfiguration courseConfiguration : configuration.getCourses()) {
            try {
                final NextcloudCourse course = new NextcloudCourse();
                course.setName(generateCourseName(courseConfiguration, configuration.getSchoolYear()).replaceAll(" ", "_"));
                course.setTeacherUsernames(courseConfiguration.getTeachers());
                course.setStudentUsernames(findCourseStudents(courseConfiguration));

                courses.add(course);
                log.info("Loaded course {} with teachers [{}] and students [{}]", course.getName(), String.join(";", course.getTeacherUsernames()), String.join(";", course.getStudentUsernames()));
            } catch (final Exception e) {
                log.error("Error loading course {}", courseConfiguration.getName(), e);
            }
        }

        log.info("Loaded {} courses", courses.size());
        log.info("Updating permission groups");

        final List<String> allGroups = nextcloudModule.getAllGroups();
        for (final NextcloudCourse course : courses) {
            try {
                updateOrCreatePermissionGroups(allGroups, course.getName() + "T", course.getTeacherUsernames());
                updateOrCreatePermissionGroups(allGroups, course.getName() + "S", course.getStudentUsernames());
            } catch (final Exception e) {
                log.error("Error updating permission groups for course {}", course.getName(), e);
            }
        }
    }

    private void updateOrCreatePermissionGroups(final List<String> allGroups, final String groupName, final List<String> newGroupMembers) throws IOException {
        if (!allGroups.contains(groupName)) {
            if (getApplication().getConfiguration().isPretendMode()) {
                log.info("PRETEND: Create group {}", groupName);
            } else {
                log.info("Creating group {}", groupName);
                nextcloudModule.createGroup(groupName);
            }
        }

        final List<String> currentGroupMembers = nextcloudModule.getGroupMembers(groupName);
        for (final String currentGroupMember : currentGroupMembers) {
            if (!newGroupMembers.contains(currentGroupMember)) {
                if (getApplication().getConfiguration().isPretendMode()) {
                    log.info("PRETEND: Remove {} from group {}", currentGroupMember, groupName);
                } else {
                    log.info("Removing {} from group {}", currentGroupMember, groupName);
                    nextcloudModule.removeGroupMember(currentGroupMember, groupName);
                }
            }
        }

        for (final String newGroupMember : newGroupMembers) {
            if (!currentGroupMembers.contains(newGroupMember)) {
                if (getApplication().getConfiguration().isPretendMode()) {
                    log.info("PRETEND: Add {} to group {}", newGroupMember, groupName);
                } else {
                    log.info("Adding {} to group {}", newGroupMember, groupName);
                    nextcloudModule.addGroupMember(newGroupMember, groupName);
                }
            }
        }
    }

    private String generateCourseName(final NextcloudCourseConfiguration courseConfiguration, final String schoolYear) throws IOException {
        final StringBuilder builder = new StringBuilder();

        for (final Integer groupId : courseConfiguration.getGroupIds()) {
            final ProcuratGroup group = procuratModule.getGroupById(groupId);
            builder.append(group.getName());

            for (final String filterValue : courseConfiguration.getStudentFilter().values()) {
                builder.append(filterValue);
            }

            builder.append("-");
        }

        builder.append(courseConfiguration.getName()).append("-");
        builder.append(schoolYear);

        return builder.toString();
    }

    private List<String> findCourseStudents(final NextcloudCourseConfiguration courseConfiguration) throws IOException {
        final List<String> students = new ArrayList<>();

        for (final Integer groupId : courseConfiguration.getGroupIds()) {
            for (final ProcuratGroupMembership membership : procuratModule.getGroupMemberships(groupId)) {
                final JsonObject data = membership.getJsonData();
                if (!data.has(nextcloudModule.getConfig().getUsernameUDF())) {
                    log.warn("Person {} is missing username UDF", membership.getPersonId());
                    continue;
                }

                if (!isFilterMatching(courseConfiguration, membership)) {
                    continue;
                }

                students.add(data.get(nextcloudModule.getConfig().getUsernameUDF()).getAsString());
            }
        }

        return students;
    }

    private boolean isFilterMatching(final NextcloudCourseConfiguration courseConfiguration, final ProcuratGroupMembership membership) {
        final JsonObject data = membership.getJsonData();

        for (final Map.Entry<String, String> entry : courseConfiguration.getStudentFilter().entrySet()) {
            if (!data.has(entry.getKey())) {
                log.warn("Person {} is missing filter key {}", membership.getPersonId(), entry.getKey());
                return false;
            }

            if (!data.get(entry.getKey()).getAsString().equals(entry.getValue())) {
                return false;
            }
        }

        return true;
    }
}
