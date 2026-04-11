package de.waldorfaugsburg.syncer.module.nextcloud;

import de.waldorfaugsburg.syncer.SyncerApplication;
import de.waldorfaugsburg.syncer.module.AbstractModule;
import de.waldorfaugsburg.syncer.module.nextcloud.model.OCSResponse;
import de.waldorfaugsburg.syncer.module.nextcloud.service.NextcloudGroupService;
import de.waldorfaugsburg.syncer.module.nextcloud.service.NextcloudUserService;
import lombok.Getter;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.io.IOException;
import java.util.List;

public class NextcloudModule extends AbstractModule {

    @Getter
    private NextcloudConfig config;
    private NextcloudGroupService groupService;
    private NextcloudUserService userService;

    public NextcloudModule(final SyncerApplication application) {
        super(application);
    }

    @Override
    public void init() throws Exception {
        config = getApplication().loadConfiguration("nextcloud.json", NextcloudConfig.class);

        final OkHttpClient client = new OkHttpClient.Builder().addInterceptor(chain -> {
            final Request.Builder builder = chain.request().newBuilder();

            builder.addHeader("Authorization", Credentials.basic(config.getUsername(), config.getPassword()));
            builder.addHeader("Accept", "application/json");
            builder.addHeader("OCS-APIRequest", "true");
            return chain.proceed(builder.build());
        }).build();

        final Retrofit retrofit = new Retrofit.Builder()
                .client(client)
                .baseUrl(config.getUrl())
                .addConverterFactory(GsonConverterFactory.create(getApplication().getGson()))
                .build();

        groupService = retrofit.create(NextcloudGroupService.class);
        userService = retrofit.create(NextcloudUserService.class);
    }

    @Override
    public void destroy() throws Exception {

    }

    public void addGroupMember(final String userId, final String groupId) throws IOException {
        userService.addGroupMember(userId, groupId).execute();
    }

    public void removeGroupMember(final String userId, final String groupId) throws IOException {
        userService.removeGroupMember(userId, groupId).execute();
    }

    public List<String> getGroupMembers(final String groupId) throws IOException {
        return getResponseData(groupService.findGroupMembers(groupId)).getUsers();
    }

    public void createGroup(final String groupId) throws IOException {
        groupService.createGroup(groupId).execute();
    }

    public List<String> getAllGroups() throws IOException {
        return getResponseData(groupService.findAllGroups()).getGroups();
    }

    private <T> T getResponseData(final Call<OCSResponse<T>> call) throws IOException {
        final Response<OCSResponse<T>> response = call.execute();
        if (!response.isSuccessful() || response.body() == null || response.body().getPayload() == null || response.body().getPayload().getData() == null) {
            throw new IOException("response malformed");
        }

        return response.body().getPayload().getData();
    }

}
