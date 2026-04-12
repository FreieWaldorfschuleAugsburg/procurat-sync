package de.waldorfaugsburg.syncer.module.ews.model;

import lombok.Data;

@Data
public class ContactModel {

    private int id;
    private String firstName;
    private String lastName;
    private String fullName;
    private String privateEmail;
    private String workEmail;
    private String note;

}
