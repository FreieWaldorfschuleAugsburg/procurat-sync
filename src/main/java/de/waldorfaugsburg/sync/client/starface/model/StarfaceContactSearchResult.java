package de.waldorfaugsburg.sync.client.starface.model;

import lombok.*;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Data
@ToString
public final class StarfaceContactSearchResult {

    private Metadata metadata;
    private List<Contact> contacts;

    @NoArgsConstructor
    @AllArgsConstructor
    @Getter
    public static class Metadata {
        private int page;
        private int totalPages;
    }

    @NoArgsConstructor
    @AllArgsConstructor
    @Getter
    public static class Contact {
        private String id;
    }
}
