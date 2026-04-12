package de.waldorfaugsburg.syncer.module.procurat.service;

import de.waldorfaugsburg.syncer.module.procurat.model.ProcuratContactInformation;
import retrofit2.Call;
import retrofit2.http.*;

import java.util.List;

public interface ProcuratContactInformationService {

    @GET("contactinformation/person/{personId}")
    Call<List<ProcuratContactInformation>> findByPersonId(@Path("personId") int personId);

    @GET("contactinformation/address/{addressId}")
    Call<List<ProcuratContactInformation>> findByAddressId(@Path("addressId") int addressId);

}
