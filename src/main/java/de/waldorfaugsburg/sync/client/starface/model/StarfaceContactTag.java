package de.waldorfaugsburg.sync.client.starface.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@NoArgsConstructor
@AllArgsConstructor
@Data
@ToString
public final class StarfaceContactTag {

    private String id;
    private String name;
    private String alias;

}
