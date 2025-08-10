package de.waldorfaugsburg.psync.client.procurat.model;

import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Data
@EqualsAndHashCode(of = "id")
@ToString
public final class ProcuratAddress {

    private int id;
    private String street;
    private int countryId;
    private String zip;
    private String city;
    private String nameline2;
    private String additional;
    private String district;
    private String poBoxZip;
    private String poBox;
    private int countyId;

}
