package de.waldorfaugsburg.syncer.module.procurat.service;

import de.waldorfaugsburg.syncer.module.procurat.model.ProcuratCommunication;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

import java.util.List;

public interface ProcuratCommunicationService {

    @GET("communication/person/{personId}/contacts")
    Call<List<ProcuratCommunication>> findByPersonId(@Path("personId") int personId);

}
