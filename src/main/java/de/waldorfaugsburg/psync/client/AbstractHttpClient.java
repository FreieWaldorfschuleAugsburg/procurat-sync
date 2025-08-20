package de.waldorfaugsburg.psync.client;

import com.google.gson.Gson;
import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.io.IOException;

public abstract class AbstractHttpClient extends AbstractClient {

    public static final Gson GSON = new Gson();
    private Retrofit retrofit;

    @Override
    protected <T extends Exception> void setup() throws T {
        final OkHttpClient client = createClient(new OkHttpClient.Builder());
        final Retrofit.Builder builder = new Retrofit.Builder();
        builder.client(client);
        builder.addConverterFactory(GsonConverterFactory.create(GSON));
        retrofit = createRetrofit(builder);
    }

    protected abstract OkHttpClient createClient(final OkHttpClient.Builder clientBuilder);

    protected abstract Retrofit createRetrofit(final Retrofit.Builder retrofitBuilder);

    public <T> T execute(final Call<T> call) throws HttpClientException {
        try {
            final Response<T> response = call.execute();
            if (!response.isSuccessful()) {
                throw new HttpClientException(response.code(), parseError(response));
            }

            return response.body();
        } catch (final IOException e) {
            throw new HttpClientException(e);
        }
    }

    private ClientError parseError(final Response<?> response) throws IOException {
        final String contentType = response.headers().get("Content-Type");
        if (contentType == null || !contentType.equals("application/json")) {
            throw new RuntimeException("invalid content type: " + contentType);
        }

        final ResponseBody errorBody = response.errorBody();
        if (errorBody == null) {
            throw new RuntimeException("unrecognized error");
        }

        final String rawBody = errorBody.string();
        errorBody.close();
        return GSON.fromJson(rawBody, ClientError.class);
    }

    protected Retrofit getRetrofit() {
        return retrofit;
    }
}
