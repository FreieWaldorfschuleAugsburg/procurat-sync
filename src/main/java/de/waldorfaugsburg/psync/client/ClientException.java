package de.waldorfaugsburg.psync.client;

import lombok.ToString;

@ToString
public class ClientException extends RuntimeException {

    private int responseCode;
    private ClientError error;

    public ClientException(final Throwable cause) {
        super(cause);
    }

    public ClientException(final int responseCode, final ClientError error) {
        this.responseCode = responseCode;
        this.error = error;
    }
}
