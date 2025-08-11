package de.waldorfaugsburg.psync.task.ews;

import de.waldorfaugsburg.psync.task.AbstractSyncTaskConfiguration;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Getter
@NoArgsConstructor
public class EWSSyncTaskConfiguration extends AbstractSyncTaskConfiguration {

    private List<ContactGroup> groups;

    @Override
    public Class<?> getTaskClass() {
        return EWSSyncTask.class;
    }

    @NoArgsConstructor
    @Getter
    public static class ContactGroup {
        private String name;
        private List<Selector> groups;
        private List<Selector> correspondenceGroups;
        private List<Selector> persons;
        private Map<String, String> extraAddresses;
    }


    @NoArgsConstructor
    @Getter
    @EqualsAndHashCode(of = {"id", "emailType"})
    public static class Selector {
        private int id;
        private String emailType;

        public Selector(final Selector selector, final int id) {
            this.id = id;
            this.emailType = selector.getEmailType();
        }
    }
}
