package de.waldorfaugsburg.psync.client.procurat.service;

import de.waldorfaugsburg.psync.client.procurat.model.ProcuratContactInformation;
import de.waldorfaugsburg.psync.client.procurat.model.ProcuratPerson;
import retrofit2.Call;
import retrofit2.http.*;

import java.util.List;

public interface ProcuratContactInformationService {

    @GET("contactinformation/person/{personId}")
    Call<List<ProcuratContactInformation>> findByPersonId(@Path("personId") int personId);

    @GET("contactinformation/address/{addressId}")
    Call<List<ProcuratContactInformation>> findByAddressId(@Path("addressId") int addressId);

}
