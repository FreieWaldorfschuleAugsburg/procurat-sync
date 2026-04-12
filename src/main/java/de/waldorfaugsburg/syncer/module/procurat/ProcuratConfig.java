package de.waldorfaugsburg.syncer.module.procurat;

import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public class ProcuratConfig {

    private String url;
    private String apiKey;
    private int rootGroupId;

}
