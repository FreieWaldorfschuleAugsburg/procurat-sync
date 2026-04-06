package de.waldorfaugsburg.syncer.module.nextcloud.service;

import de.waldorfaugsburg.syncer.module.nextcloud.model.OCSGroupListData;
import de.waldorfaugsburg.syncer.module.nextcloud.model.OCSGroupMemberListData;
import de.waldorfaugsburg.syncer.module.nextcloud.model.OCSResponse;
import retrofit2.Call;
import retrofit2.http.*;

public interface NextcloudGroupService {

    @GET("ocs/v1.php/cloud/groups")
    Call<OCSResponse<OCSGroupListData>> findAllGroups();

    @POST("ocs/v1.php/cloud/groups")
    @FormUrlEncoded
    Call<Void> createGroup(@Field("groupid") final String groupId);

    @GET("ocs/v1.php/cloud/groups/{groupId}")
    Call<OCSResponse<OCSGroupMemberListData>> findGroupMembers(@Path("groupId") final String groupId);

}
