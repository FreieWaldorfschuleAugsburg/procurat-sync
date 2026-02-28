package de.waldorfaugsburg.syncer.module.starface.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@NoArgsConstructor
@AllArgsConstructor
@Data
@ToString
public final class StarfaceLogin {

    private String loginType;
    private String nonce;
    private String secret;

}
