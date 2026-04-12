package de.waldorfaugsburg.syncer.module.nextcloud.model;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public class OCSMeta {

    private String status;
    @SerializedName("statuscode")
    private int statusCode;
    private String message;
    @SerializedName("totalitems")
    private String totalItems;
    @SerializedName("itemsperpage")
    private String itemsPerPage;

}
