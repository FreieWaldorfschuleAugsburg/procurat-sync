package de.waldorfaugsburg.psync.client.procurat.model;

import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Data
@EqualsAndHashCode(of = "id")
@ToString
public final class ProcuratUDF {

    private int id;
    private int groupId;
    private String groupType;
    private String groupBaseType;
    private String name;
    private String fieldType;
    private String usage;
    private boolean learning;
    private String referenceTable;
    private String description;
    private boolean active;
    private int sortIndex;

}
