package de.waldorfaugsburg.syncer.module.nextcloud.service;

import retrofit2.Call;
import retrofit2.http.*;

public interface NextcloudUserService {

    @POST("ocs/v1.php/cloud/users/{userId}")
    @FormUrlEncoded
    Call<Void> addGroupMember(@Path("userId") final String userId, @Field("groupid") final String groupId);

    @DELETE("ocs/v1.php/cloud/users/{userId}")
    @FormUrlEncoded
    Call<Void> removeGroupMember(@Path("userId") final String userId, @Field("groupid") final String groupId);

}
