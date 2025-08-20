package de.waldorfaugsburg.psync.task.activedirectory;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.gson.JsonElement;
import de.waldorfaugsburg.psync.ProcuratSyncApplication;
import de.waldorfaugsburg.psync.client.activedirectory.ADClient;
import de.waldorfaugsburg.psync.client.activedirectory.model.ADUser;
import de.waldorfaugsburg.psync.client.procurat.ProcuratClient;
import de.waldorfaugsburg.psync.client.procurat.model.ProcuratCommunication;
import de.waldorfaugsburg.psync.client.procurat.model.ProcuratContactInformation;
import de.waldorfaugsburg.psync.client.procurat.model.ProcuratGroupMembership;
import de.waldorfaugsburg.psync.client.procurat.model.ProcuratPerson;
import de.waldorfaugsburg.psync.task.AbstractSyncTask;
import lombok.extern.slf4j.Slf4j;

import javax.naming.NamingException;
import java.util.*;

@Slf4j
public final class ADSyncTask extends AbstractSyncTask<ADSyncTaskConfiguration> {

    public ADSyncTask(final ProcuratSyncApplication application, final ADSyncTaskConfiguration configuration) {
        super(application, configuration);
    }

    @Override
    public void run() throws Exception {
        final ProcuratClient procuratClient = new ProcuratClient(getApplication());
        final ADClient adClient = new ADClient(getApplication());

        final List<ProcuratGroupMembership> rootGroupMemberships = procuratClient.getRootGroupMemberships();
        final Multimap<ADSyncTaskConfiguration.UserMapper, ADSyncTaskConfiguration.Selector> selectorGroupMap = ArrayListMultimap.create();
        for (final ADSyncTaskConfiguration.UserMapper mapper : getConfiguration().getUserMappers()) {
            selectorGroupMap.putAll(mapper, accumulateSelectors(procuratClient, mapper).stream().distinct().toList());
        }

        final long personCount = selectorGroupMap.values().stream().distinct().count();
        log.info("Aggregated a total of {} persons in {} groups for synchronisation", personCount, selectorGroupMap.keySet().size());

        final Set<Integer> processedPersonIds = new HashSet<>();
        for (final ADSyncTaskConfiguration.UserMapper mapper : selectorGroupMap.keySet()) {
            final Collection<ADSyncTaskConfiguration.Selector> selectors = selectorGroupMap.get(mapper);
            log.info("Starting with mapper {} ({} members)", mapper.getName(), selectors.size());

            for (final ADSyncTaskConfiguration.Selector selector : selectors) {
                ADUser adUser = adClient.findUserByEmployeeId(selector.getId());
                if (!processedPersonIds.contains(selector.getId())) {
                    final ProcuratPerson person = procuratClient.getPersonById(selector.getId());

                    try {
                        createOrUpdateADUser(adClient, procuratClient, rootGroupMemberships, mapper, adUser, person);
                        adUser = adClient.findUserByEmployeeId(selector.getId());
                    } catch (final Exception e) {
                        recordDeviation("Unable to update/create AD user for person '%s' (Id: %s): %s",
                                person.getId(), person.getFirstName() + " " + person.getLastName(),
                                e.getMessage());
                    }

                    // Add to processed persons
                    processedPersonIds.add(selector.getId());

                    if (adUser == null) {
                        continue;
                    }
                }

                // Add to mapper group
                for (final String targetGroup : mapper.getTargetGroups()) {
                    if (!adClient.isGroupMember(adUser, targetGroup)) {
                        adClient.addToGroup(adUser, targetGroup);
                    }
                }
            }
        }

        for (final ADUser adUser : adClient.findAllUsers()) {
            if (adUser.isDisabled() || adUser.getEmployeeId() == null) continue;

            if (procuratClient.isPersonInactive(rootGroupMemberships, adUser.getEmployeeId())) {
                adClient.disableUser(adUser);
                continue;
            }

            if (adUser.mustChangePassword()) {
                final ProcuratPerson person = procuratClient.getPersonById(adUser.getEmployeeId());
                recordDeviation("Pending password change for user %s (Username: %s | Id: %s)",
                        person.getLastName() + " " + person.getFirstName(),
                        adUser.getSAMAccountName(), person.getId());
            }
        }
    }

    private void createOrUpdateADUser(final ADClient adClient, final ProcuratClient procuratClient,
                                      final List<ProcuratGroupMembership> rootMemberships, final ADSyncTaskConfiguration.UserMapper mapper,
                                      final ADUser adUser, final ProcuratPerson person) throws NamingException {
        ProcuratGroupMembership rootMembership = null;
        for (final ProcuratGroupMembership membership : rootMemberships) {
            if (membership.getPersonId() == person.getId()) {
                rootMembership = membership;
                break;
            }
        }

        if (rootMembership == null) {
            throw new IllegalStateException("inactive person " + person.getId());
        }

        // Get username from UDF
        final String usernameUDF = getApplication().getConfiguration().getClients().getActiveDirectory().getUsernameUDF();
        final JsonElement usernameElement = rootMembership.getJsonData().get(usernameUDF);
        if (usernameElement == null) {
            throw new IllegalStateException("no username in UDF for person " + person.getId());
        }

        final String username = usernameElement.getAsString();

        // Get UPN from ContactInformation
        final List<ProcuratContactInformation> personContactInformation = procuratClient.getContactInformationByPersonId(person.getId());
        String upn = null;
        for (final ProcuratContactInformation information : personContactInformation) {
            if (information.getMedium().equals("email") && information.getType().equals("work") && !information.isSecret()) {
                upn = information.getContent();
            }
        }

        if (upn == null) {
            throw new IllegalStateException("no UPN found for person " + person.getId());
        }

        // Create user if null
        if (adUser == null) {
            final String password = "Start" + person.getId() + "#" + person.getId();
            adClient.createUser(mapper.getTargetDN(), person.getId(), username, upn, person.getFirstName(),
                    person.getLastName(), password, mapper.getTitle(), mapper.getOffice(), mapper.getDescription());
            return;
        }

        // Inform about lazy users
        if (adUser.isDisabled()) {
            recordDeviation("Updating disabled AD user '%s' (Username: %s | Id: %s)",
                    adUser.getCn(), adUser.getSAMAccountName(), adUser.getEmployeeId());
        }

        adClient.updateUser(adUser, upn, person.getFirstName(), person.getLastName(), mapper.getTitle(), mapper.getOffice(), mapper.getDescription());
    }

    private List<ADSyncTaskConfiguration.Selector> accumulateSelectors(final ProcuratClient client, final ADSyncTaskConfiguration.UserMapper mapper) {
        final List<ADSyncTaskConfiguration.Selector> selectors = new ArrayList<>();

        // Add group members
        for (final ADSyncTaskConfiguration.Selector selector : mapper.getGroups()) {
            for (final ProcuratGroupMembership membership : client.getGroupMemberships(selector.getId())) {
                selectors.add(new ADSyncTaskConfiguration.Selector(selector, membership.getPersonId()));
            }
        }

        // Add correspondence persons for group members
        for (final ADSyncTaskConfiguration.Selector selector : mapper.getCorrespondenceGroups()) {
            for (final ProcuratGroupMembership membership : client.getGroupMemberships(selector.getId())) {
                for (final ProcuratCommunication communication : client.getCommunicationsByPersonId(membership.getPersonId())) {
                    selectors.add(new ADSyncTaskConfiguration.Selector(selector, communication.getContactPersonId()));
                }
            }
        }

        // Specific persons
        selectors.addAll(mapper.getPersons());
        return selectors;
    }
}
