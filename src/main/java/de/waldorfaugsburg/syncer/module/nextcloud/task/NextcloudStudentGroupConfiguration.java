package de.waldorfaugsburg.syncer.module.nextcloud.task;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

@Getter
@NoArgsConstructor
public class NextcloudStudentGroupConfiguration {

    private String name;
    private int groupId;
    private Map<String, String> udfFilter;

}
