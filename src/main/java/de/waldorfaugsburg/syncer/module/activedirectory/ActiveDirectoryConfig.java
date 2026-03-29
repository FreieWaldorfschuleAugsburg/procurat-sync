package de.waldorfaugsburg.syncer.module.activedirectory;

import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public class ActiveDirectoryConfig {

    private String domain;
    private String host;
    private String principal;
    private String password;

}
