package de.waldorfaugsburg.syncer.module.nextcloud.task;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class NextcloudCourseConfiguration {

    private String name;
    private List<String> teachers;
    @SerializedName("students")
    private List<String> studentGroups;

}
