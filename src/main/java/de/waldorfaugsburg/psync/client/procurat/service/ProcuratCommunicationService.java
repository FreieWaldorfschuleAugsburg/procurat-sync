package de.waldorfaugsburg.psync.client.procurat.service;

import de.waldorfaugsburg.psync.client.procurat.model.ProcuratCommunication;
import de.waldorfaugsburg.psync.client.procurat.model.ProcuratContactInformation;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

import java.util.List;

public interface ProcuratCommunicationService {

    @GET("communication/person/{personId}/contacts")
    Call<List<ProcuratCommunication>> findByPersonId(@Path("personId") int personId);

}
