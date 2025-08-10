package de.waldorfaugsburg.psync.config;

import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.NoArgsConstructor;
import okhttp3.internal.concurrent.Task;

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
    public static class TaskConfiguration {
        private String name;
        private String interval;
        private JsonObject custom;
    }
}
