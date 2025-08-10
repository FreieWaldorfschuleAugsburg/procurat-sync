package de.waldorfaugsburg.psync.client.procurat.service;

import de.waldorfaugsburg.psync.client.procurat.model.ProcuratAddress;
import retrofit2.Call;
import retrofit2.http.*;

public interface ProcuratAddressService {

    @GET("addresses/{addressId}")
    Call<ProcuratAddress> findById(@Path("addressId") int addressId);

}
