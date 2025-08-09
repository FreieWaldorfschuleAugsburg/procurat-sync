package de.waldorfaugsburg.psync.client.procurat.service;

import de.waldorfaugsburg.psync.client.procurat.model.ProcuratGroup;
import de.waldorfaugsburg.psync.client.procurat.model.ProcuratGroupMembership;
import de.waldorfaugsburg.psync.client.procurat.model.ProcuratUDF;
import retrofit2.Call;
import retrofit2.http.*;

import java.util.List;

public interface ProcuratGroupService {

    @GET("groups")
    Call<ProcuratGroup> findAll();

    @GET("groups")
    Call<List<ProcuratGroup>> findGroupsByMemberId(@Query("memberId") int memberId);

    @GET("groups/{groupId}")
    Call<ProcuratGroup> findGroupById(@Path("id") int groupId);

    @GET("groups/{groupId}/members")
    Call<List<ProcuratGroupMembership>> findGroupMembersByGroupId(@Path("groupId") int groupId);

    @PUT("groups/{groupId}/members/{personId}")
    Call<Void> updateGroupMembership(@Path("groupId") int groupId, @Path("personId") int personId, @Body ProcuratGroupMembership membership);

    @POST("groups/{groupId}/members")
    Call<Void> createGroupMembership(@Path("groupId") int groupId, @Body ProcuratGroupMembership membership);

    @GET("groups/{groupId}/udfs")
    Call<List<ProcuratUDF>> findUDFsByGroupId(@Path("groupId") int groupId, @Query("includeParentGroups") boolean includeParentGroups);

}
