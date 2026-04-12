package de.waldorfaugsburg.syncer.module.ews.task.addressbook;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

@Getter
@NoArgsConstructor
public class EWSContactGroupConfiguration {

    private int id;
    private String displayName;
    private Map<String, String> groups;
    private Map<String, String> correspondenceGroups;
    private Map<String, String> persons;
    private Map<String, String> oneOffAddresses;

}
