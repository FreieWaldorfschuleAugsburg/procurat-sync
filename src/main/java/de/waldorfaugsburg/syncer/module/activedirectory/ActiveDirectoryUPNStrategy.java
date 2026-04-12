package de.waldorfaugsburg.syncer.module.activedirectory;

import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public class ActiveDirectoryUPNStrategy {

    private String domain;
    private boolean internal;

}
