package de.waldorfaugsburg.syncer.client.procurat;

import de.waldorfaugsburg.syncer.SyncerApplication;
import de.waldorfaugsburg.syncer.client.AbstractHttpClient;
import de.waldorfaugsburg.syncer.client.HttpClientException;
import de.waldorfaugsburg.syncer.module.procurat.model.*;
import de.waldorfaugsburg.syncer.module.procurat.service.*;
import lombok.Getter;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

public final class ProcuratClient extends AbstractHttpClient {

    private static final Duration TIMEOUT_DURATION = Duration.ofMinutes(5);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssX");

    private final String url;
    private final String apiKey;
    private final int rootGroupId;
    private final Map<String, Integer> namedGroups;

    @Getter
    private ProcuratPersonService personService;
    @Getter
    private ProcuratGroupService groupService;
    @Getter
    private ProcuratContactInformationService contactInformationService;
    @Getter
    private ProcuratAddressService addressService;
    @Getter
    private ProcuratCommunicationService communicationService;

    ProcuratClient(final SyncerApplication application) {
        this.url = application.getConfiguration().getClients().getProcurat().getUrl();
        this.apiKey = application.getConfiguration().getClients().getProcurat().getApiKey();
        this.rootGroupId = application.getConfiguration().getClients().getProcurat().getRootGroupId();
        this.namedGroups = application.getConfiguration().getClients().getProcurat().getNamedGroups();
    }

    public static ProcuratClient createInstance(final SyncerApplication application) throws HttpClientException {
        final ProcuratClient client = new ProcuratClient(application);
        client.setup();
        return client;
    }

    @Override
    protected void setup() {
        super.setup();

        this.personService = getRetrofit().create(ProcuratPersonService.class);
        this.groupService = getRetrofit().create(ProcuratGroupService.class);
        this.contactInformationService = getRetrofit().create(ProcuratContactInformationService.class);
        this.addressService = getRetrofit().create(ProcuratAddressService.class);
        this.communicationService = getRetrofit().create(ProcuratCommunicationService.class);
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

    public ProcuratPerson getPersonById(final int personId) throws HttpClientException {
        return execute(personService.findById(personId));
    }

    public List<ProcuratPerson> getAllPersons() throws HttpClientException {
        return execute(personService.findAll());
    }

    public List<ProcuratPerson> getPersonsByFamilyId(final int familyId) throws HttpClientException {
        return execute(personService.findByFamilyId(familyId));
    }

    public List<ProcuratGroupMembership> getGroupMemberships(final int groupId) throws HttpClientException {
        return execute(groupService.findMembers(groupId));
    }

    public List<ProcuratGroupMembership> getRootGroupMemberships() throws HttpClientException {
        return getGroupMemberships(rootGroupId);
    }

    public boolean isPersonInactive(final List<ProcuratGroupMembership> memberships, final int personId) {
        for (final ProcuratGroupMembership membership : memberships) {
            if (membership.getPersonId() == personId) {
                final LocalDateTime now = LocalDateTime.now();
                final LocalDateTime entryDate = LocalDateTime.parse(membership.getEntryDate(), FORMATTER);
                final LocalDateTime exitDate = membership.getExitDate() == null ? null : LocalDateTime.parse(membership.getExitDate(), FORMATTER);

                // Check if membership is still active
                if (now.isAfter(entryDate) && (exitDate == null || now.isBefore(exitDate))) {
                    return false;
                }
            }
        }
        return true;
    }

    public String getNamedGroupName(final int personId) throws HttpClientException {
        for (final String name : namedGroups.keySet()) {
            final Integer groupId = namedGroups.get(name);
            for (final ProcuratGroupMembership membership : getGroupMemberships(groupId)) {
                if (membership.getPersonId() == personId) {
                    return name;
                }
            }
        }
        return null;
    }

    public List<ProcuratContactInformation> getContactInformationByPersonId(final int personId) throws HttpClientException {
        return execute(contactInformationService.findByPersonId(personId));
    }

    public List<ProcuratContactInformation> getContactInformationByAddressId(final int addressId) throws HttpClientException {
        return execute(contactInformationService.findByAddressId(addressId));
    }

    public ProcuratAddress getAddressById(final int addressId) throws HttpClientException {
        return execute(addressService.findById(addressId));
    }

    public List<ProcuratCommunication> getCommunicationsByPersonId(final int personId) throws HttpClientException {
        return execute(communicationService.findByPersonId(personId));
    }
}
