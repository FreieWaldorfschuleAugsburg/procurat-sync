package de.waldorfaugsburg.psync.client.procurat;

import de.waldorfaugsburg.psync.client.AbstractHttpClient;
import de.waldorfaugsburg.psync.client.procurat.model.ProcuratGroupMembership;
import de.waldorfaugsburg.psync.client.procurat.model.ProcuratPerson;
import de.waldorfaugsburg.psync.client.procurat.service.ProcuratContactInformationService;
import de.waldorfaugsburg.psync.client.procurat.service.ProcuratGroupService;
import de.waldorfaugsburg.psync.client.procurat.service.ProcuratPersonService;
import lombok.Getter;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public final class ProcuratClient extends AbstractHttpClient {

    private static final Duration TIMEOUT_DURATION = Duration.ofMinutes(5);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssX");

    private final String url;
    private final String apiKey;
    private final int rootGroupId;

    @Getter
    private ProcuratPersonService personService;
    @Getter
    private ProcuratGroupService groupService;
    @Getter
    private ProcuratContactInformationService contactInformationService;

    public ProcuratClient(final String url, final String apiKey, int rootGroupId) {
        this.url = url;
        this.apiKey = apiKey;
        this.rootGroupId = rootGroupId;

        setup();
    }

    @Override
    protected void setup() {
        super.setup();

        this.personService = getRetrofit().create(ProcuratPersonService.class);
        this.groupService = getRetrofit().create(ProcuratGroupService.class);
        this.contactInformationService = getRetrofit().create(ProcuratContactInformationService.class);
    }

    public List<ProcuratGroupMembership> getRootGroupMemberships() {
        return execute(groupService.findMembers(rootGroupId));
    }

    public boolean isPersonActiveMember(final List<ProcuratGroupMembership> memberships, final ProcuratPerson person) {
        for (final ProcuratGroupMembership membership : memberships) {
            if (membership.getPersonId() == person.getId()) {
                final LocalDateTime now = LocalDateTime.now();
                final LocalDateTime entryDate = LocalDateTime.parse(membership.getEntryDate(), FORMATTER);
                final LocalDateTime exitDate = membership.getExitDate() == null ? null : LocalDateTime.parse(membership.getExitDate(), FORMATTER);

                if (now.isAfter(entryDate) && (exitDate == null || now.isBefore(exitDate))) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    protected OkHttpClient createClient(final OkHttpClient.Builder clientBuilder) {
        clientBuilder.addInterceptor(chain -> chain.proceed(chain.request().newBuilder().addHeader("X-API-KEY", apiKey).build()));
        clientBuilder.callTimeout(TIMEOUT_DURATION).connectTimeout(TIMEOUT_DURATION).readTimeout(TIMEOUT_DURATION).writeTimeout(TIMEOUT_DURATION);
        return clientBuilder.build();
    }

    @Override
    protected Retrofit createRetrofit(final Retrofit.Builder retrofitBuilder) {
        retrofitBuilder.baseUrl(this.url);
        return retrofitBuilder.build();
    }
}
