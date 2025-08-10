package de.waldorfaugsburg.psync.client.starface.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Data
@ToString
public final class StarfaceContactBlock {

    private String name;
    private String resourceKey;
    private List<StarfaceContactAttribute> attributes;

}
