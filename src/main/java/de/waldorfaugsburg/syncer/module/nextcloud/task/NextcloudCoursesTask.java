package de.waldorfaugsburg.syncer.module.nextcloud.task;

import com.google.gson.JsonObject;
import de.waldorfaugsburg.syncer.SyncerApplication;
import de.waldorfaugsburg.syncer.module.nextcloud.NextcloudModule;
import de.waldorfaugsburg.syncer.module.nextcloud.model.OCSFolderData;
import de.waldorfaugsburg.syncer.module.procurat.ProcuratModule;
import de.waldorfaugsburg.syncer.module.procurat.model.ProcuratGroupMembership;
import de.waldorfaugsburg.syncer.task.AbstractScheduledTask;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class NextcloudCoursesTask extends AbstractScheduledTask {

    private static final int READ_ONLY_PERMISSION = 1;
    private static final int WRITE_PERMISSION = 15;
    private static final int FULL_PERMISSION = 31;

    private NextcloudCoursesTaskConfiguration configuration;
    private ProcuratModule procuratModule;
    private NextcloudModule nextcloudModule;

    public NextcloudCoursesTask(final SyncerApplication application) {
        super(application, "nextcloud_courses.json", NextcloudCoursesTaskConfiguration.class, ProcuratModule.class, NextcloudModule.class);
    }

    @Override
    public void run() throws Exception {
        procuratModule = getApplication().getModuleRegistry().getInstance(ProcuratModule.class);
        nextcloudModule = getApplication().getModuleRegistry().getInstance(NextcloudModule.class);
        nextcloudModule.login();

        final List<String> allGroups = nextcloudModule.getAllGroups();
        final Map<String, OCSFolderData> allFoldersMap = new HashMap<>();
        for (final Map.Entry<String, OCSFolderData> entry : nextcloudModule.getAllFolders().entrySet()) {
            final OCSFolderData folderData = entry.getValue();
            allFoldersMap.put(folderData.getMountPoint(), folderData);
        }

        configuration = (NextcloudCoursesTaskConfiguration) getConfiguration();

        log.info("Updating student groups");

        for (final NextcloudStudentGroupConfiguration studentGroupConfiguration : configuration.getStudentGroups()) {
            try {
                final String studentGroupName = (studentGroupConfiguration.getName() + "-" + configuration.getSchoolYear()).replaceAll(" ", "_");
                final List<String> studentUsernames = findStudents(studentGroupConfiguration);

                // Update or create student group
                updateOrCreatePermissionGroups(allGroups, studentGroupName, studentUsernames);

                log.info("Updated student group {}", studentGroupName);
            } catch (final Exception e) {
                log.error("Error updating student group {}", studentGroupConfiguration.getName(), e);
            }
        }

        log.info("Updating courses");

        for (final NextcloudCourseConfiguration courseConfiguration : configuration.getCourses()) {
            try {
                final String courseName = generateCourseName(courseConfiguration);

                // Update or create teacher group
                updateOrCreatePermissionGroups(allGroups, courseName, courseConfiguration.getTeachers());

                // Create main folder
                final Map<String, Integer> studentsRO = new HashMap<>();
                studentsRO.put(courseName, FULL_PERMISSION);
                for (final String studentGroup : courseConfiguration.getStudentGroups()) {
                    studentsRO.put(generateStudentGroupName(studentGroup), READ_ONLY_PERMISSION);
                }
                updateOrCreateFolder(allFoldersMap, courseName, studentsRO);

                // Create sharing folder
                final Map<String, Integer> allRW = new HashMap<>();
                allRW.put(courseName, FULL_PERMISSION);
                for (final String studentGroup : courseConfiguration.getStudentGroups()) {
                    allRW.put(generateStudentGroupName(studentGroup), WRITE_PERMISSION);
                }
                updateOrCreateFolder(allFoldersMap, courseName + "/_AUSTAUSCH", allRW);

                // Create teacher only folder
                updateOrCreateFolder(allFoldersMap, courseName + "/_LEHRKRAFT", Map.of(courseName, FULL_PERMISSION));

                log.info("Updated course {} with teachers [{}] and student groups [{}]", courseName, String.join(";", courseConfiguration.getTeachers()), String.join(";", courseConfiguration.getStudentGroups()));
            } catch (final Exception e) {
                log.error("Error loading course {}", courseConfiguration.getName(), e);
            }
        }
    }

    private void updateOrCreateFolder(final Map<String, OCSFolderData> allFoldersMap, final String folderName, final Map<String, Integer> newPermissions) throws IOException {
        OCSFolderData folderData = allFoldersMap.get(folderName);
        if (folderData == null) {
            if (!getApplication().getConfiguration().isPretendMode()) {
                folderData = nextcloudModule.createFolder(folderName);
            }

            if (folderData == null && !getApplication().getConfiguration().isPretendMode()) {
                throw new IllegalStateException("folder not created");
            }
        }

        final Map<String, Integer> currentPermissions = folderData != null ? folderData.getGroupPermissions() : new HashMap<>();
        for (final Map.Entry<String, Integer> entry : currentPermissions.entrySet()) {
            if (!newPermissions.containsKey(entry.getKey())) {
                if (getApplication().getConfiguration().isPretendMode()) {
                    log.info("PRETEND: Remove group {} permission from folder {}", entry.getKey(), folderName);
                } else {
                    log.info("Removing group {} permission from folder {}", entry.getKey(), folderName);
                    nextcloudModule.removeFolderGroupPermission(folderData.getId(), entry.getKey());
                }
            } else if (!newPermissions.get(entry.getKey()).equals(entry.getValue())) {
                if (getApplication().getConfiguration().isPretendMode()) {
                    log.info("PRETEND: Update group {} permission to folder {} with value {}", entry.getKey(), folderName, entry.getValue());
                } else {
                    log.info("Updating group {} permission to folder {} with value {}", entry.getKey(), folderName, entry.getValue());
                    nextcloudModule.removeFolderGroupPermission(folderData.getId(), entry.getKey());
                    nextcloudModule.addFolderGroupPermission(folderData.getId(), entry.getKey(), entry.getValue());
                }
            }
        }

        for (final Map.Entry<String, Integer> entry : newPermissions.entrySet()) {
            if (!currentPermissions.containsKey(entry.getKey())) {
                if (getApplication().getConfiguration().isPretendMode()) {
                    log.info("PRETEND: Add group {} permission to folder {} with value {}", entry.getKey(), folderName, entry.getValue());
                } else {
                    log.info("Adding group {} permission to folder {} with value {}", entry.getKey(), folderName, entry.getValue());
                    nextcloudModule.addFolderGroupPermission(folderData.getId(), entry.getKey(), entry.getValue());
                }
            }
        }

        log.info("Updated folder {}", folderName);
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

        final List<String> currentGroupMembers = !allGroups.contains(groupName) && getApplication().getConfiguration().isPretendMode() ? new ArrayList<>() : nextcloudModule.getGroupMembers(groupName);
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

    private String generateCourseName(final NextcloudCourseConfiguration courseConfiguration) {
        final StringBuilder builder = new StringBuilder();
        for (final String studentGroups : courseConfiguration.getStudentGroups()) {
            builder.append(studentGroups).append("-");
        }
        builder.append(courseConfiguration.getName()).append("-");
        builder.append(configuration.getSchoolYear());

        return builder.toString().replaceAll(" ", "_");
    }

    private String generateStudentGroupName(final String groupName) {
        return (groupName + "-" + configuration.getSchoolYear()).replaceAll(" ", "_");
    }

    private List<String> findStudents(final NextcloudStudentGroupConfiguration configuration) throws IOException {
        final List<String> students = new ArrayList<>();

        for (final ProcuratGroupMembership membership : procuratModule.getGroupMemberships(configuration.getGroupId())) {
            final JsonObject data = membership.getJsonData();
            if (!data.has(nextcloudModule.getConfig().getUsernameUDF())) {
                log.warn("Person {} is missing username UDF", membership.getPersonId());
                continue;
            }

            if (!isFilterMatching(configuration, membership)) {
                continue;
            }

            students.add(data.get(nextcloudModule.getConfig().getUsernameUDF()).getAsString());
        }

        return students;
    }

    private boolean isFilterMatching(final NextcloudStudentGroupConfiguration configuration, final ProcuratGroupMembership membership) {
        final JsonObject data = membership.getJsonData();

        for (final Map.Entry<String, String> entry : configuration.getUdfFilter().entrySet()) {
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
