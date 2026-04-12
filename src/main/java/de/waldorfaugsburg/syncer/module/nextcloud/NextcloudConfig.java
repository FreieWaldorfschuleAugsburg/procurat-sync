package de.waldorfaugsburg.syncer.module.nextcloud;

import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public class NextcloudConfig {

    private String url;
    private String username;
    private String password;
    private String usernameUDF;

}
