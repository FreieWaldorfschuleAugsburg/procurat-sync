package de.waldorfaugsburg.psync.client.procurat.service;

import de.waldorfaugsburg.psync.client.procurat.model.ProcuratPerson;
import retrofit2.Call;
import retrofit2.http.*;

import java.util.List;

public interface ProcuratPersonService {

    @GET("persons/{personId}")
    Call<ProcuratPerson> findPersonById(@Path("personId") int personId);

    @GET("persons/family/{personId}")
    Call<List<ProcuratPerson>> findPersonsByFamilyId(@Path("id") int personId);

    @GET("persons")
    Call<List<ProcuratPerson>> findAll();

    @PUT("persons/{personId}")
    Call<Void> updatePerson(@Path("personId") int personId, @Body ProcuratPerson person);

    @POST("persons")
    Call<Void> createPerson(@Body ProcuratPerson person);

}
