package de.waldorfaugsburg.psync.config;

import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor
@Getter
public final class ApplicationConfiguration {

    private ClientConfiguration clients;
    private List<TaskConfiguration> tasks;

    @NoArgsConstructor
    @Getter

    public static class ClientConfiguration {
        private ProcuratClientConfiguration procurat;
        private StarfaceClientConfiguration starface;
        private EWSClientConfiguration ews;
    }

    @NoArgsConstructor
    @Getter
    public static class ProcuratClientConfiguration {
        private String url;
        private String apiKey;
        private int rootGroupId;
    }

    @NoArgsConstructor
    @Getter
    public static class StarfaceClientConfiguration {
        private String url;
        private String userId;
        private String password;
        private String tag;
    }

    @NoArgsConstructor
    @Getter
    public static class EWSClientConfiguration {
        private String clientId;
        private String tenantId;
        private String clientSecret;
        private String contactFolderId;
        private String impersonatedUserId;
    }

    @NoArgsConstructor
    @Getter
    public static class TaskConfiguration {
        private String type;
        private String interval;
        private JsonObject custom;
    }
}
