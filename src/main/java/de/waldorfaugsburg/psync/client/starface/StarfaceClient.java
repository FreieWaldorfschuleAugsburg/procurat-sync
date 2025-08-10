package de.waldorfaugsburg.psync.client.starface;

import de.waldorfaugsburg.psync.client.AbstractHttpClient;
import de.waldorfaugsburg.psync.client.starface.model.*;
import de.waldorfaugsburg.psync.client.starface.service.StarfaceService;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.apache.commons.codec.digest.DigestUtils;
import retrofit2.Retrofit;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public final class StarfaceClient extends AbstractHttpClient {

    private final String url;
    private final String userId;
    private final String password;
    private final String tagName;

    private StarfaceService service;
    private String authToken;
    private StarfaceContactTag tag;

    public StarfaceClient(final String url, final String userId, final String password, final String tagName) {
        this.url = url;
        this.userId = userId;
        this.password = password;
        this.tagName = tagName;

        setup();
    }

    @Override
    protected void setup() {
        super.setup();

        service = getRetrofit().create(StarfaceService.class);
        login();

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
        for (int i = 0; i < Integer.min(phoneNumbers.size(), 4); i++) {
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
            StarfaceContactSearchResult result = execute(service.findContacts(tag.getId(), page, 40));

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
        if (login == null || login.getNonce() == null) return;

        final String hashedPassword = DigestUtils.sha512Hex(password);
        final String loginSecret = userId + ":" + DigestUtils.sha512Hex(userId + login.getNonce() + hashedPassword);
        login.setSecret(loginSecret);

        final StarfaceToken token = execute(service.login(login));
        if (token == null || token.getToken() == null) return;
        authToken = token.getToken();
        log.info("Login successful");
    }
}
