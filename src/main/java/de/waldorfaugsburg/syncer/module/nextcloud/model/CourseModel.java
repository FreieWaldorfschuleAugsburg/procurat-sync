package de.waldorfaugsburg.syncer.module.nextcloud.model;

import lombok.Data;

import java.util.List;

@Data
public class CourseModel {

    private String name;
    private List<String> teacherUsernames;
    private List<String> studentUsernames;

}
