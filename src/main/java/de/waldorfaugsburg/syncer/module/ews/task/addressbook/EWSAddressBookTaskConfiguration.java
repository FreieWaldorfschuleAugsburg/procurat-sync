package de.waldorfaugsburg.syncer.module.ews.task.addressbook;

import de.waldorfaugsburg.syncer.task.ScheduledTaskConfiguration;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class EWSAddressBookTaskConfiguration extends ScheduledTaskConfiguration {

    private List<EWSContactGroupConfiguration> groups;

}
