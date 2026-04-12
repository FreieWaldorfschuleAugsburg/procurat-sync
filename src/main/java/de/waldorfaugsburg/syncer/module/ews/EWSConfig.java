package de.waldorfaugsburg.syncer.module.ews;

import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public class EWSConfig {

    private String clientId;
    private String tenantId;
    private String clientSecret;
    private String contactFolderId;
    private String impersonatedUserId;

}
