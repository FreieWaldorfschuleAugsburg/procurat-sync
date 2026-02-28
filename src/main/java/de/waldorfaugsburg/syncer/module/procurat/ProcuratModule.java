package de.waldorfaugsburg.syncer.module.procurat;

import de.waldorfaugsburg.syncer.SyncerApplication;
import de.waldorfaugsburg.syncer.module.AbstractModule;
import de.waldorfaugsburg.syncer.module.procurat.model.*;
import de.waldorfaugsburg.syncer.module.procurat.service.*;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ProcuratModule extends AbstractModule {

    private static final Duration TIMEOUT_DURATION = Duration.ofMinutes(5);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssX");

    private ProcuratConfig config;

    private ProcuratPersonService personService;
    private ProcuratGroupService groupService;
    private ProcuratContactInformationService contactInformationService;
    private ProcuratAddressService addressService;
    private ProcuratCommunicationService communicationService;

    public ProcuratModule(final SyncerApplication application) {
        super(application);
    }

    @Override
    public void init() throws Exception {
        config = getApplication().loadConfiguration("procurat.json", ProcuratConfig.class);

        final OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(chain -> chain.proceed(chain.request().newBuilder().addHeader("X-API-KEY", config.getApiKey()).build()))
                .callTimeout(TIMEOUT_DURATION).connectTimeout(TIMEOUT_DURATION).readTimeout(TIMEOUT_DURATION).writeTimeout(TIMEOUT_DURATION)
                .build();

        final Retrofit retrofit = new Retrofit.Builder()
                .client(client)
                .baseUrl(config.getUrl())
                .addConverterFactory(GsonConverterFactory.create(getApplication().getGson()))
                .build();

        personService = retrofit.create(ProcuratPersonService.class);
        groupService = retrofit.create(ProcuratGroupService.class);
        contactInformationService = retrofit.create(ProcuratContactInformationService.class);
        addressService = retrofit.create(ProcuratAddressService.class);
        communicationService = retrofit.create(ProcuratCommunicationService.class);
    }

    @Override
    public void destroy() throws Exception {

    }

    public boolean isPersonInactive(final List<ProcuratGroupMembership> memberships, final int personId) {
        final LocalDateTime now = LocalDateTime.now();

        for (final ProcuratGroupMembership membership : memberships) {
            if (membership.getPersonId() == personId) {
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

    public ProcuratPerson getPersonById(final int personId) throws IOException {
        return personService.findById(personId).execute().body();
    }

    public List<ProcuratPerson> getAllPersons() throws IOException {
        return personService.findAll().execute().body();
    }

    public List<ProcuratPerson> getPersonsByFamilyId(final int familyId) throws IOException {
        return personService.findByFamilyId(familyId).execute().body();
    }

    public List<ProcuratGroupMembership> getGroupMemberships(final int groupId) throws IOException {
        return groupService.findMembers(groupId).execute().body();
    }

    public List<ProcuratGroupMembership> getRootGroupMemberships() throws IOException {
        return getGroupMemberships(config.getRootGroupId());
    }

    public List<ProcuratContactInformation> getContactInformationByPersonId(final int personId) throws IOException {
        return contactInformationService.findByPersonId(personId).execute().body();
    }

    public List<ProcuratContactInformation> getContactInformationByAddressId(final int addressId) throws IOException {
        return contactInformationService.findByAddressId(addressId).execute().body();
    }

    public ProcuratAddress getAddressById(final int addressId) throws IOException {
        return addressService.findById(addressId).execute().body();
    }

    public List<ProcuratCommunication> getCommunicationsByPersonId(final int personId) throws IOException {
        return communicationService.findByPersonId(personId).execute().body();
    }
}
