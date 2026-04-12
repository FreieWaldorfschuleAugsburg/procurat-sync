package de.waldorfaugsburg.syncer.module.nextcloud.service;

import retrofit2.Call;
import retrofit2.http.*;

public interface NextcloudService {

    @POST("login")
    Call<Void> login();

}
