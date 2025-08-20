package de.waldorfaugsburg.psync.client.procurat;

import de.waldorfaugsburg.psync.ProcuratSyncApplication;
import de.waldorfaugsburg.psync.client.AbstractHttpClient;
import de.waldorfaugsburg.psync.client.procurat.model.*;
import de.waldorfaugsburg.psync.client.procurat.service.*;
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

    public ProcuratClient(final ProcuratSyncApplication application) {
        this.url = application.getConfiguration().getClients().getProcurat().getUrl();
        this.apiKey = application.getConfiguration().getClients().getProcurat().getApiKey();
        this.rootGroupId = application.getConfiguration().getClients().getProcurat().getRootGroupId();
        this.namedGroups = application.getConfiguration().getClients().getProcurat().getNamedGroups();

        setup();
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

    public ProcuratPerson getPersonById(final int personId) {
        return execute(personService.findById(personId));
    }

    public List<ProcuratPerson> getAllPersons() {
        return execute(personService.findAll());
    }

    public List<ProcuratPerson> getPersonsByFamilyId(final int familyId) {
        return execute(personService.findByFamilyId(familyId));
    }

    public List<ProcuratGroupMembership> getGroupMemberships(final int groupId) {
        return execute(groupService.findMembers(groupId));
    }

    public List<ProcuratGroupMembership> getRootGroupMemberships() {
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

    public String getNamedGroupName(final int personId) {
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

    public List<ProcuratContactInformation> getContactInformationByPersonId(final int personId) {
        return execute(contactInformationService.findByPersonId(personId));
    }

    public List<ProcuratContactInformation> getContactInformationByAddressId(final int addressId) {
        return execute(contactInformationService.findByAddressId(addressId));
    }

    public ProcuratAddress getAddressById(final int addressId) {
        return execute(addressService.findById(addressId));
    }

    public List<ProcuratCommunication> getCommunicationsByPersonId(final int personId) {
        return execute(communicationService.findByPersonId(personId));
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
