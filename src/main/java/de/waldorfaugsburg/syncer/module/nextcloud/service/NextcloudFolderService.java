package de.waldorfaugsburg.syncer.module.nextcloud.service;

import com.google.gson.JsonObject;
import de.waldorfaugsburg.syncer.module.nextcloud.model.OCSFolderData;
import de.waldorfaugsburg.syncer.module.nextcloud.model.OCSResponse;
import retrofit2.Call;
import retrofit2.http.*;

import java.util.Map;

public interface NextcloudFolderService {

    @GET("apps/groupfolders/folders")
    Call<OCSResponse<Map<String, OCSFolderData>>> findAllFolders();

    @POST("apps/groupfolders/folders")
    Call<OCSResponse<OCSFolderData>> createFolder(@Body final JsonObject body);

    @POST("apps/groupfolders/folders/{id}/groups")
    Call<Void> addFolderGroupAccess(@Path("id") final int folderId, @Body final JsonObject body);

    @POST("apps/groupfolders/folders/{id}/groups/{group}")
    Call<Void> addFolderGroupPermission(@Path("id") final int folderId, @Path("group") final String group, @Body final JsonObject body);

    @DELETE("apps/groupfolders/folders/{id}/groups/{group}")
    Call<Void> removeFolderGroupAccess(@Path("id") final int folderId, @Path("group") final String group);

}
