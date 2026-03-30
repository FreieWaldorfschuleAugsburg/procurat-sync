package de.waldorfaugsburg.syncer.module.activedirectory.task;

import com.google.common.collect.*;
import de.waldorfaugsburg.syncer.SyncerApplication;
import de.waldorfaugsburg.syncer.module.activedirectory.ActiveDirectoryModule;
import de.waldorfaugsburg.syncer.module.activedirectory.ActiveDirectoryUPNStrategy;
import de.waldorfaugsburg.syncer.module.activedirectory.model.ActiveDirectoryAttribute;
import de.waldorfaugsburg.syncer.module.activedirectory.model.ActiveDirectoryUser;
import de.waldorfaugsburg.syncer.module.procurat.ProcuratModule;
import de.waldorfaugsburg.syncer.module.procurat.model.ProcuratCommunication;
import de.waldorfaugsburg.syncer.module.procurat.model.ProcuratContactInformation;
import de.waldorfaugsburg.syncer.module.procurat.model.ProcuratGroupMembership;
import de.waldorfaugsburg.syncer.module.procurat.model.ProcuratPerson;
import de.waldorfaugsburg.syncer.task.AbstractScheduledTask;
import lombok.extern.slf4j.Slf4j;

import javax.naming.NamingException;
import java.io.IOException;
import java.util.*;

@Slf4j
public class ActiveDirectoryTask extends AbstractScheduledTask {

    private final HashMultimap<String, String> groupMemberMultimap = HashMultimap.create();

    private ProcuratModule procuratModule;
    private ActiveDirectoryModule activeDirectoryModule;

    public ActiveDirectoryTask(final SyncerApplication application) {
        super(application, "ad_task.json", ActiveDirectoryTaskConfiguration.class, ProcuratModule.class, ActiveDirectoryModule.class);
    }

    @Override
    public void run() throws Exception {
        procuratModule = getApplication().getModuleRegistry().getInstance(ProcuratModule.class);
        activeDirectoryModule = getApplication().getModuleRegistry().getInstance(ActiveDirectoryModule.class);

        final TreeMultimap<Integer, ActiveDirectoryMapper> personMapperMultimap = TreeMultimap.create(Ordering.natural(),
                Comparator.comparingInt(ActiveDirectoryMapper::getPriority));

        log.info("Preparing mappers");

        final ActiveDirectoryTaskConfiguration taskConfiguration = (ActiveDirectoryTaskConfiguration) getConfiguration();
        for (final ActiveDirectoryMapper mapper : taskConfiguration.getMappers()) {
            try {
                final Set<Integer> personIds = findMapperPersons(mapper);
                for (final Integer personId : personIds) {
                    personMapperMultimap.put(personId, mapper);
                }

                log.info("Prepared mapper (name: {}, persons: {})", mapper.getName(), personIds.size());
            } catch (final Exception e) {
                log.error("Error while loading mapper (name: {})", mapper.getName(), e);
            }
        }

        log.info("Executing mappers");

        for (final Integer personId : personMapperMultimap.keySet()) {
            boolean primary = true;

            for (final ActiveDirectoryMapper mapper : personMapperMultimap.get(personId)) {
                try {
                    log.debug("Execute mapper (personId: {}, mapper: {})", personId, mapper.getName());
                    executeMapper(personId, mapper, primary);
                } catch (final Exception e) {
                    log.error("Error while handling person (mapper: {}, personId: {})", mapper.getName(), personId, e);
                }

                primary = false;
            }
        }

        log.info("Update group members");

        for (final String groupDN : groupMemberMultimap.keySet()) {
            final Set<String> desiredMembers = groupMemberMultimap.get(groupDN);
            final Set<String> actualMembers = activeDirectoryModule.getGroupMembers(groupDN);

            for (final String userDN : actualMembers) {
                if (!desiredMembers.contains(userDN)) {
                    activeDirectoryModule.removeGroupMember(groupDN, userDN);
                }
            }

            for (final String userDN : desiredMembers) {
                if (!actualMembers.contains(userDN)) {
                    activeDirectoryModule.addGroupMember(groupDN, userDN);
                }
            }
        }

        // Clear map for next run
        groupMemberMultimap.clear();

        log.info("Disable inactive users");

        final Map<Integer, ProcuratGroupMembership> rootGroupMembershipsMap = procuratModule.getRootGroupMembershipsMap();
        for (final ActiveDirectoryUser user : activeDirectoryModule.findAllUsers()) {
            if (user.isDisabled()) {
                continue;
            }

            final String personId = user.getAttribute(ActiveDirectoryAttribute.EMPLOYEE_ID);
            if (personId == null) {
                continue;
            }

            if (!rootGroupMembershipsMap.containsKey(Integer.parseInt(personId))) {
                user.setDisabled(true);
                log.info("Disabled inactive user (personId: {})", personId);
            }
        }
    }

    private Set<Integer> findMapperPersons(final ActiveDirectoryMapper mapper) throws IOException {
        final Set<Integer> personIds = new HashSet<>();

        for (final int groupId : mapper.getGroups()) {
            for (final ProcuratGroupMembership membership : procuratModule.getGroupMemberships(groupId)) {
                personIds.add(membership.getPersonId());
            }
        }

        for (final int groupId : mapper.getCorrespondenceGroups()) {
            for (final ProcuratGroupMembership membership : procuratModule.getGroupMemberships(groupId)) {
                for (final ProcuratCommunication communication : procuratModule.getCommunicationsByPersonId(membership.getPersonId())) {
                    personIds.add(communication.getContactPersonId());
                }
            }
        }

        personIds.addAll(mapper.getPersons());
        return personIds;
    }

    private void executeMapper(final int personId, final ActiveDirectoryMapper mapper, final boolean primary) throws NamingException, IOException {
        final ProcuratPerson person = procuratModule.getPersonById(personId);

        boolean newUser = false;
        ActiveDirectoryUser user = activeDirectoryModule.findUserByEmployeeId(personId);
        if (user == null) {
            if (!primary) {
                throw new IllegalStateException("user null in non-primary mapper");
            }

            user = createUser(person, mapper);
            newUser = true;
        }

        // Update static values so execution exists if user was e.g. renamed
        user.setAttribute(ActiveDirectoryAttribute.CN, person.getFullName());
        user.setAttribute(ActiveDirectoryAttribute.DISPLAY_NAME, person.getFullName());
        user.setAttribute(ActiveDirectoryAttribute.GIVEN_NAME, person.getFirstName());
        user.setAttribute(ActiveDirectoryAttribute.SN, person.getLastName());

        // If is primary mapper
        final String distinguishedName = user.getAttribute(ActiveDirectoryAttribute.DN);
        if (primary && distinguishedName == null || primary && distinguishedName.contains(mapper.getTargetDN())) {
            user.setAttribute(ActiveDirectoryAttribute.TITLE, mapper.getTitle());
            user.setAttribute(ActiveDirectoryAttribute.PHYSICAL_DELIVERY_OFFICE_NAME, mapper.getOffice());
            user.setAttribute(ActiveDirectoryAttribute.DESCRIPTION, mapper.getDescription());

            final ActiveDirectoryUPNStrategy upnStrategy = activeDirectoryModule.getUpnStrategy(mapper.getUpnStrategy());
            if (upnStrategy == null) {
                throw new IllegalStateException("invalid upn strategy");
            }

            if (upnStrategy.isInternal()) {
                for (final ProcuratContactInformation info : procuratModule.getContactInformationByPersonId(personId)) {
                    if (info.getOrder() == 1 && info.getMedium().equals("email") && info.getType().equals("private")) {
                        user.setAttribute(ActiveDirectoryAttribute.MAIL, info.getContent());
                    }
                }

                if (user.getAttribute(ActiveDirectoryAttribute.MAIL) == null) {
                    throw new IllegalStateException("no mail found");
                }
            } else {
                user.setAttribute(ActiveDirectoryAttribute.MAIL, user.getAttribute(ActiveDirectoryAttribute.USER_PRINCIPAL_NAME));
            }
        }

        if (newUser) {
            activeDirectoryModule.createUser(mapper.getTargetDN(), user);
            procuratModule.updateRootGroupUDF(personId, activeDirectoryModule.getConfig().getUsernameUDF(), user.getAttribute(ActiveDirectoryAttribute.SAM_ACCOUNT_NAME));
        } else {
            activeDirectoryModule.updateUser(user);
        }

        for (final String targetGroupDN : mapper.getTargetGroups()) {
            groupMemberMultimap.put(targetGroupDN, user.getAttribute(ActiveDirectoryAttribute.DN));
        }
    }

    private ActiveDirectoryUser createUser(final ProcuratPerson person, final ActiveDirectoryMapper mapper) throws NamingException {
        final ActiveDirectoryUPNStrategy upnStrategy = activeDirectoryModule.getUpnStrategy(mapper.getUpnStrategy());
        if (upnStrategy == null) {
            throw new IllegalStateException("invalid upn strategy");
        }

        final ActiveDirectoryUser user = new ActiveDirectoryUser();
        user.setAttribute(ActiveDirectoryAttribute.EMPLOYEE_ID, Integer.toString(person.getId()));
        user.setAttribute(ActiveDirectoryAttribute.SAM_ACCOUNT_NAME, activeDirectoryModule.generateUsername(person.getFirstName(), person.getLastName()));

        final String userPart;
        if (upnStrategy.isInternal()) {
            userPart = user.getAttribute(ActiveDirectoryAttribute.SAM_ACCOUNT_NAME);
        } else {
            userPart = activeDirectoryModule.normalizeInput(person.getFirstName()) + "." + activeDirectoryModule.normalizeInput(person.getLastName());
        }

        user.setAttribute(ActiveDirectoryAttribute.USER_PRINCIPAL_NAME, userPart + "@" + upnStrategy.getDomain());
        user.setAttribute(ActiveDirectoryAttribute.USER_ACCOUNT_CONTROL, "512");
        user.setPassword("Start" + person.getId() + "#" + person.getId());
        user.setAttribute(ActiveDirectoryAttribute.PWD_LAST_SET, "0");

        return user;
    }
}
