package de.waldorfaugsburg.syncer.module.ews.model;

import com.google.common.collect.Multimap;
import lombok.Data;

@Data
public class ContactGroupModel {

    private int id;
    private String displayName;
    private Multimap<String, String> addresses;

}
