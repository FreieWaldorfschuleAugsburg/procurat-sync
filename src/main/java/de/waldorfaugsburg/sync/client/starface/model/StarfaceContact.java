package de.waldorfaugsburg.sync.client.starface.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Data
@ToString
public final class StarfaceContact {

    private List<StarfaceContactTag> tags;
    private List<StarfaceContactBlock> blocks;

}
