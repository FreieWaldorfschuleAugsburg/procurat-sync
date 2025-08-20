package de.waldorfaugsburg.psync.config;

import de.waldorfaugsburg.psync.task.AbstractSyncTaskConfiguration;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@NoArgsConstructor
@Getter
public final class ApplicationConfiguration {

    private ClientConfiguration clients;
    private List<AbstractSyncTaskConfiguration> tasks;
    private MailConfiguration mail;

    @NoArgsConstructor
    @Getter
    public static class MailConfiguration {
        private String host;
        private int port;
        private String username;
        private String password;
        private List<String> recipients;
    }

    @NoArgsConstructor
    @Getter
    public static class ClientConfiguration {
        private ProcuratClientConfiguration procurat;
        private StarfaceClientConfiguration starface;
        private EWSClientConfiguration ews;
        private ActiveDirectoryConfiguration activeDirectory;
    }

    @NoArgsConstructor
    @Getter
    public static class ProcuratClientConfiguration {
        private String url;
        private String apiKey;
        private int rootGroupId;
        private Map<String, Integer> namedGroups;
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
    public static class ActiveDirectoryConfiguration {
        private String domain;
        private String host;
        private String principal;
        private String password;
        private String userDN;
        private String usernameUDF;
        private String upnUDF;
    }
}
