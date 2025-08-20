package de.waldorfaugsburg.psync.task.activedirectory;

import de.waldorfaugsburg.psync.task.AbstractSyncTaskConfiguration;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public final class ADSyncTaskConfiguration extends AbstractSyncTaskConfiguration {

    private List<UserMapper> userMappers;

    @NoArgsConstructor
    @Getter
    public static class UserMapper {
        private String name;
        private List<Selector> groups;
        private List<Selector> correspondenceGroups;
        private List<Selector> persons;
        private String targetDN;
        private List<String> targetGroups;
        private String title;
        private String office;
        private String description;
    }

    @NoArgsConstructor
    @Getter
    @EqualsAndHashCode(of = {"id"})
    public static class Selector {
        private int id;

        public Selector(final Selector selector, final int id) {
            this.id = id;
        }
    }

    @Override
    public Class<?> getTaskClass() {
        return ADSyncTask.class;
    }
}
