package de.waldorfaugsburg.syncer.module.procurat.service;

import de.waldorfaugsburg.syncer.module.procurat.model.ProcuratAddress;
import retrofit2.Call;
import retrofit2.http.*;

public interface ProcuratAddressService {

    @GET("addresses/{addressId}")
    Call<ProcuratAddress> findById(@Path("addressId") int addressId);

}
