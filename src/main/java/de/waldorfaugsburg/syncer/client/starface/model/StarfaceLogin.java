package de.waldorfaugsburg.syncer.client.starface.model;

import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Data
@ToString
public final class StarfaceLogin {

    private String loginType;
    private String nonce;
    private String secret;

}
