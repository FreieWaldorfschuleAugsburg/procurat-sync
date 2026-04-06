package de.waldorfaugsburg.syncer.module.nextcloud.model;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor
@Getter
public class OCSGroupListData {

    private List<String> groups;

}
