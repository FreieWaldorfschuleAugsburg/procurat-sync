package de.waldorfaugsburg.psync.client.starface.service;

import de.waldorfaugsburg.psync.client.starface.model.*;
import retrofit2.Call;
import retrofit2.http.*;

import java.util.List;

public interface StarfaceService {

    @GET("login")
    Call<StarfaceLogin> requestLogin();

    @POST("login")
    Call<StarfaceToken> login(@Body StarfaceLogin login);

    @GET("contacts")
    Call<StarfaceContactSearchResult> findContacts(@Query("tags") final String tagIds, @Query("page") final int page, @Query("pagesize") final int pageSize);

    @POST("contacts")
    Call<Void> createContact(@Body StarfaceContact contact);

    @DELETE("contacts/{contactId}")
    Call<Void> deleteContact(@Path("contactId") String contactId);

    @GET("contacts/tags")
    Call<List<StarfaceContactTag>> findAllTags();

}

