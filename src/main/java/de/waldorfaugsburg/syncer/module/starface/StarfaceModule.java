package de.waldorfaugsburg.syncer.module.starface;

import de.waldorfaugsburg.syncer.SyncerApplication;
import de.waldorfaugsburg.syncer.module.AbstractModule;
import de.waldorfaugsburg.syncer.module.starface.exception.StarfaceException;
import de.waldorfaugsburg.syncer.module.starface.model.*;
import de.waldorfaugsburg.syncer.module.starface.service.StarfaceService;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.apache.commons.codec.digest.DigestUtils;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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
    private StarfaceContactTag contactTag;

    public StarfaceModule(final SyncerApplication application) {
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

        final Retrofit retrofit = new Retrofit.Builder()
                .client(client)
                .baseUrl(config.getUrl())
                .addConverterFactory(GsonConverterFactory.create(getApplication().getGson()))
                .build();

        service = retrofit.create(StarfaceService.class);

        login();
        contactTag = findTagByAlias(config.getTag());
        if (contactTag == null) {
            throw new StarfaceException("tag not found");
        }
    }

    @Override
    public void destroy() throws Exception {

    }

    public void createContact(final String firstName, final String lastName, final String homePhoneNumber,
                              final List<String> phoneNumbers) throws IOException, StarfaceException {
        if (isTokenInvalid()) {
            login();
        }

        final List<StarfaceContactTag> tags = new ArrayList<>();
        tags.add(contactTag);

        final List<StarfaceContactBlock> blocks = new ArrayList<>();

        final StarfaceContactBlock contactBlock = new StarfaceContactBlock();
        contactBlock.setName("contact");
        contactBlock.setResourceKey("de.vertico.starface.addressbook.block.label_contact");
        final List<StarfaceContactAttribute> contactAttributes = new ArrayList<>();
        contactAttributes.add(new StarfaceContactAttribute("NAME", "firstname", firstName,
                "de.vertico.starface.addressbook.line.label_firstname"));
        contactAttributes.add(new StarfaceContactAttribute("SURNAME", "familyname", lastName,
                "de.vertico.starface.addressbook.line.label_lastname"));
        contactBlock.setAttributes(contactAttributes);

        blocks.add(contactBlock);

        final StarfaceContactBlock telephoneBlock = new StarfaceContactBlock();
        telephoneBlock.setName("telephone");
        telephoneBlock.setResourceKey("de.vertico.starface.addressbook.block.label_telephone");
        final List<StarfaceContactAttribute> telephoneAttributes = new ArrayList<>();
        if (homePhoneNumber != null) {
            telephoneAttributes.add(new StarfaceContactAttribute("PRIVATE_PHONE_NUMBER",
                    "homephone", homePhoneNumber, "de.vertico.starface.addressbook.line.label_privatetelephonenumber"));
        }

        // STARFACE supports a maximum of 4 phone numbers per contact
        for (int i = 0; i < Integer.min(phoneNumbers.size(), MAX_NUMBERS_PER_CONTACT); i++) {
            final String telephoneNumber = phoneNumbers.get(i);
            telephoneAttributes.add(new StarfaceContactAttribute("PHONE_NUMBER",
                    i == 0 ? "phone" : ("phone" + (i + 1)), telephoneNumber, "de.vertico.starface.addressbook.line.label_telephonenumber"));
        }

        telephoneBlock.setAttributes(telephoneAttributes);
        blocks.add(telephoneBlock);

        final StarfaceContact contact = new StarfaceContact();
        contact.setTags(tags);
        contact.setBlocks(blocks);

        service.createContact(contact).execute();
    }

    public int deleteAllContacts() throws IOException, StarfaceException {
        if (isTokenInvalid()) {
            login();
        }

        final StarfaceContactSearchResult metadataResult = service.findContacts(contactTag.getId(), 0, 40).execute().body();
        if (metadataResult == null) {
            throw new StarfaceException("response invalid");
        }

        int count = 0;
        for (int page = metadataResult.getMetadata().getTotalPages(); page >= 0; page--) {
            final StarfaceContactSearchResult result = service.findContacts(contactTag.getId(), page, 40).execute().body();
            if (result == null) {
                throw new StarfaceException("response invalid");
            }

            for (final StarfaceContactSearchResult.Contact contact : result.getContacts()) {
                service.deleteContact(contact.getId()).execute();
                count++;
            }
        }

        return count;
    }

    private void login() throws IOException, StarfaceException {
        final StarfaceLogin login = service.requestLogin().execute().body();
        if (login == null || login.getNonce() == null || login.getNonce().isEmpty()) {
            throw new StarfaceException("nonce invalid");
        }

        final String hashedPassword = DigestUtils.sha512Hex(config.getPassword());
        final String loginSecret = config.getUserId() + ":" + DigestUtils.sha512Hex(config.getUserId() + login.getNonce() + hashedPassword);
        login.setSecret(loginSecret);

        final StarfaceTokenResponse tokenResponse = service.login(login).execute().body();
        if (tokenResponse == null || tokenResponse.getToken() == null) {
            throw new StarfaceException("token invalid");
        }

        token = tokenResponse.getToken();
        loginTimestamp = System.currentTimeMillis();

        log.info("Logged in as {}", config.getUserId());
    }

    private StarfaceContactTag findTagByAlias(final String alias) throws IOException, StarfaceException {
        if (isTokenInvalid()) {
            login();
        }

        final List<StarfaceContactTag> tags = service.findAllTags().execute().body();
        if (tags != null) {
            for (final StarfaceContactTag tag : tags) {
                if (tag.getAlias().equals(alias)) return tag;
            }
        }

        return null;
    }

    private boolean isTokenInvalid() {
        return System.currentTimeMillis() - loginTimestamp >= TOKEN_MAX_VALIDITY_MILLIS;
    }
}
