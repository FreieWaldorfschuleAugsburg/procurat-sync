package de.waldorfaugsburg.syncer.module.starface.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@NoArgsConstructor
@AllArgsConstructor
@Data
@ToString
public final class StarfaceContactAttribute {

    private String displayKey;
    private String name;
    private String value;
    private String i18nDisplayName;

}
