package de.waldorfaugsburg.syncer.module.nextcloud.model;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public class OCSResponse<T> {

    @SerializedName("ocs")
    private OCSPayload<T> payload;

    @NoArgsConstructor
    @Getter
    public static class OCSPayload<T> {
        private OCSMeta meta;
        private T data;
    }

}
