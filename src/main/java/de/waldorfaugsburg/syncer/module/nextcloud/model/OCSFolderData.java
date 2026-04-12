package de.waldorfaugsburg.syncer.module.nextcloud.model;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@NoArgsConstructor
@Getter
public class OCSFolderData {

    private int id;
    @SerializedName("mount_point")
    private String mountPoint;
    @SerializedName("groups")
    private Map<String, Integer> groupPermissions;

}
