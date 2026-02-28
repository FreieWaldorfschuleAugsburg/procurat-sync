package de.waldorfaugsburg.syncer.config;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor
@Getter
public final class ApplicationConfiguration {

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
}
