package de.waldorfaugsburg.psync.client.procurat.model;

import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Data
@EqualsAndHashCode(of = "id")
@ToString
public final class ProcuratCommunication {

    private int id;
    private int personId;
    private int contactPersonId;
    private boolean isEmergency;
    private boolean includeAddressOnList;
    private boolean includeHomePhoneOnList;

}
