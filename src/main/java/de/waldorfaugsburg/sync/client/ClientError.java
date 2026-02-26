package de.waldorfaugsburg.sync.client;

import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Data
@EqualsAndHashCode()
@ToString
public final class ClientError {
    private String code;
    private String message;
}
