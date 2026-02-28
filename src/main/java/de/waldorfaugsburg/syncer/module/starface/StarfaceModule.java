package de.waldorfaugsburg.syncer.module.starface;

import de.waldorfaugsburg.syncer.SyncerApplication;
import de.waldorfaugsburg.syncer.module.AbstractModule;
import de.waldorfaugsburg.syncer.module.starface.exception.StarfaceLoginException;
import de.waldorfaugsburg.syncer.module.starface.model.StarfaceLogin;
import de.waldorfaugsburg.syncer.module.starface.model.StarfaceTokenResponse;
import de.waldorfaugsburg.syncer.module.starface.service.StarfaceService;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.apache.commons.codec.digest.DigestUtils;
import retrofit2.Retrofit;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Slf4j
public class StarfaceModule extends AbstractModule {

    // STARFACE can't display more than 4 phone numbers per contact
    private static final int MAX_NUMBERS_PER_CONTACT = 4;

    // Documentation states 4 hours validity for a token - but just in case
    // see more: https://knowledge.starface.de/display/SWD/REST+-+Schnittstelle
    private static final long TOKEN_MAX_VALIDITY_MILLIS = TimeUnit.HOURS.toMillis(3);

    private StarfaceConfig config;
    private StarfaceService service;

    private String token;
    private long loginTimestamp;

    protected StarfaceModule(final SyncerApplication application) {
        super(application);
    }

    @Override
    public void init() throws Exception {
        config = getApplication().loadConfiguration("starface.json", StarfaceConfig.class);

        final OkHttpClient client = new OkHttpClient.Builder().addInterceptor(chain -> {
            final Request.Builder builder = chain.request().newBuilder();
            if (token != null) {
                builder.addHeader("authToken", token);
            }
            builder.addHeader("Content-Type", "application/json");
            builder.addHeader("Accept", "application/json");
            builder.addHeader("X-Version", "2");
            return chain.proceed(builder.build());
        }).build();

        final Retrofit retrofit = new Retrofit.Builder().baseUrl(config.getUrl()).build();
        service = retrofit.create(StarfaceService.class);

        login();
    }

    @Override
    public void destroy() throws Exception {

    }

    private void login() throws IOException, StarfaceLoginException {
        final StarfaceLogin login = service.requestLogin().execute().body();
        if (login == null || login.getNonce() == null || login.getNonce().isEmpty()) {
            throw new StarfaceLoginException("nonce invalid");
        }

        final String hashedPassword = DigestUtils.sha512Hex(config.getPassword());
        final String loginSecret = config.getUserId() + ":" + DigestUtils.sha512Hex(config.getUserId() + login.getNonce() + hashedPassword);
        login.setSecret(loginSecret);

        final StarfaceTokenResponse tokenResponse = service.login(login).execute().body();
        if (tokenResponse == null || tokenResponse.getToken() == null) {
            throw new StarfaceLoginException("token invalid");
        }

        token = tokenResponse.getToken();
        loginTimestamp = System.currentTimeMillis();

        log.info("Logged in as {}", config.getUserId());
    }

    private boolean isTokenInvalid() {
        return System.currentTimeMillis() - loginTimestamp >= TOKEN_MAX_VALIDITY_MILLIS;
    }
}
