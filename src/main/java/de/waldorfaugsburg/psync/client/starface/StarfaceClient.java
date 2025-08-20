package de.waldorfaugsburg.psync.client.starface;

import de.waldorfaugsburg.psync.ProcuratSyncApplication;
import de.waldorfaugsburg.psync.client.AbstractHttpClient;
import de.waldorfaugsburg.psync.client.ClientException;
import de.waldorfaugsburg.psync.client.starface.model.*;
import de.waldorfaugsburg.psync.client.starface.service.StarfaceService;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.apache.commons.codec.digest.DigestUtils;
import retrofit2.Call;
import retrofit2.Retrofit;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * STARFACE REST Client implementation as per official documentation
 *
 * @see <a href="https://knowledge.starface.de/display/SWD/REST+-+Schnittstelle">STARFACE REST documentation</a>
 **/
@Slf4j
public final class StarfaceClient extends AbstractHttpClient {

    // STARFACE can't display more than 4 phone numbers per contact
    private static final int MAX_NUMBERS_PER_CONTACT = 4;

    // Documentation states 4 hours validity for a token - but just in case
    private static final long TOKEN_MAX_VALIDITY_MILLIS = TimeUnit.HOURS.toMillis(3);

    private final String url;
    private final String userId;
    private final String password;
    private final String tagName;

    private StarfaceService service;
    private String authToken;
    private StarfaceContactTag tag;

    private long loginMillis;

    public StarfaceClient(final ProcuratSyncApplication application) {
        this.url = application.getConfiguration().getClients().getStarface().getUrl();
        this.userId = application.getConfiguration().getClients().getStarface().getUserId();
        this.password = application.getConfiguration().getClients().getStarface().getPassword();
        this.tagName = application.getConfiguration().getClients().getStarface().getTag();

        setup();
    }

    @Override
    protected void setup() {
        super.setup();
        service = getRetrofit().create(StarfaceService.class);

        try {
            login();
        } catch (final Exception e) {
            log.error("Login failed", e);
        }

        tag = findTagByAlias(tagName);
    }

    @Override
    protected OkHttpClient createClient(final OkHttpClient.Builder clientBuilder) {
        clientBuilder.addInterceptor(chain -> {
            final Request.Builder builder = chain.request().newBuilder();
            if (authToken != null) {
                builder.addHeader("authToken", authToken);
            }
            builder.addHeader("Content-Type", "application/json");
            builder.addHeader("Accept", "application/json");
            builder.addHeader("X-Version", "2");
            return chain.proceed(builder.build());
        });
        return clientBuilder.build();
    }

    @Override
    protected Retrofit createRetrofit(final Retrofit.Builder retrofitBuilder) {
        retrofitBuilder.baseUrl(this.url);
        return retrofitBuilder.build();
    }

    @Override
    public <T> T execute(Call<T> call) throws ClientException {
        // If token is invalid and call isn't a login call
        if (!isTokenValid() && !call.request().url().toString().endsWith("login")) {
            log.info("Requesting new token due to expiration");
            login();
        }

        return super.execute(call);
    }

    public StarfaceContactTag findTagByAlias(final String alias) {
        final List<StarfaceContactTag> tags = execute(service.findAllTags());
        for (final StarfaceContactTag tag : tags) {
            if (tag.getAlias().equals(alias)) return tag;
        }
        return null;
    }

    public void createContact(final String firstName, final String lastName, final String homePhoneNumber, final List<String> phoneNumbers) {
        final List<StarfaceContactTag> tags = new ArrayList<>();
        tags.add(tag);

        final List<StarfaceContactBlock> blocks = new ArrayList<>();

        final StarfaceContactBlock contactBlock = new StarfaceContactBlock();
        contactBlock.setName("contact");
        contactBlock.setResourceKey("de.vertico.starface.addressbook.block.label_contact");
        final List<StarfaceContactAttribute> contactAttributes = new ArrayList<>();
        contactAttributes.add(new StarfaceContactAttribute("NAME", "firstname", firstName, "de.vertico.starface.addressbook.line.label_firstname"));
        contactAttributes.add(new StarfaceContactAttribute("SURNAME", "familyname", lastName, "de.vertico.starface.addressbook.line.label_lastname"));
        contactBlock.setAttributes(contactAttributes);

        blocks.add(contactBlock);

        final StarfaceContactBlock telephoneBlock = new StarfaceContactBlock();
        telephoneBlock.setName("telephone");
        telephoneBlock.setResourceKey("de.vertico.starface.addressbook.block.label_telephone");
        final List<StarfaceContactAttribute> telephoneAttributes = new ArrayList<>();
        if (homePhoneNumber != null) {
            telephoneAttributes.add(new StarfaceContactAttribute("PRIVATE_PHONE_NUMBER", "homephone", homePhoneNumber, "de.vertico.starface.addressbook.line.label_privatetelephonenumber"));
        }

        // STARFACE supports a maximum of 4 phone numbers per contact
        for (int i = 0; i < Integer.min(phoneNumbers.size(), MAX_NUMBERS_PER_CONTACT); i++) {
            final String telephoneNumber = phoneNumbers.get(i);
            telephoneAttributes.add(new StarfaceContactAttribute("PHONE_NUMBER", i == 0 ? "phone" : ("phone" + (i + 1)), telephoneNumber, "de.vertico.starface.addressbook.line.label_telephonenumber"));
        }

        telephoneBlock.setAttributes(telephoneAttributes);
        blocks.add(telephoneBlock);

        final StarfaceContact contact = new StarfaceContact();
        contact.setTags(tags);
        contact.setBlocks(blocks);

        execute(service.createContact(contact));
        log.info("Created contact (name: {})", firstName + " " + lastName);
    }

    public void deleteAllContacts() {
        final StarfaceContactSearchResult metadataResult = execute(service.findContacts(tag.getId(), 0, 40));

        int count = 0;
        for (int page = metadataResult.getMetadata().getTotalPages(); page >= 0; page--) {
            final StarfaceContactSearchResult result = execute(service.findContacts(tag.getId(), page, 40));

            for (final StarfaceContactSearchResult.Contact contact : result.getContacts()) {
                execute(service.deleteContact(contact.getId()));
                count++;
                log.info("Deleted contact (id: {})", contact.getId());
            }
        }

        log.info("Deleted {} contacts", count);
    }

    private void login() {
        final StarfaceLogin login = execute(service.requestLogin());
        if (login == null || login.getNonce() == null)
            throw new IllegalStateException("nonce invalid");

        final String hashedPassword = DigestUtils.sha512Hex(password);
        final String loginSecret = userId + ":" + DigestUtils.sha512Hex(userId + login.getNonce() + hashedPassword);
        login.setSecret(loginSecret);

        final StarfaceToken token = execute(service.login(login));
        if (token == null || token.getToken() == null)
            throw new IllegalStateException("token invalid");

        authToken = token.getToken();
        loginMillis = System.currentTimeMillis();

        log.info("Login successful");
    }

    private boolean isTokenValid() {
        return System.currentTimeMillis() - loginMillis < TOKEN_MAX_VALIDITY_MILLIS;
    }
}
