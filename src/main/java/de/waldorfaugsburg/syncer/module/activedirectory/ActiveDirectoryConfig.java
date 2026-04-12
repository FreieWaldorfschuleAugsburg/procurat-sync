package de.waldorfaugsburg.syncer.module.activedirectory;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

@NoArgsConstructor
@Getter
public class ActiveDirectoryConfig {

    private String domain;
    private String host;
    private String principal;
    private String password;
    private String usersDN;
    private String usernamePrefix;
    private String usernameUDF;
    private Map<String, ActiveDirectoryUPNStrategy> upnStrategies;

}
