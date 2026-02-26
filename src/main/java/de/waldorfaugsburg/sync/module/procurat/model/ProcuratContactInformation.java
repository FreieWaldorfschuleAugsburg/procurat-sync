package de.waldorfaugsburg.sync.module.procurat.model;

import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Data
@EqualsAndHashCode(of = "id")
@ToString
public final class ProcuratContactInformation {

    private int id;
    private int order;
    private String type;
    private String medium;
    private int personId;
    private int addressId;
    private String externalName;
    private String content;
    private String comment;
    private boolean secret;

}
