package de.waldorfaugsburg.sync;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import de.waldorfaugsburg.sync.config.ApplicationConfiguration;
import de.waldorfaugsburg.sync.config.JsonAdapter;
import de.waldorfaugsburg.sync.mail.ApplicationMailer;
import de.waldorfaugsburg.sync.task.AbstractSyncTaskConfiguration;
import de.waldorfaugsburg.sync.task.SyncTaskScheduler;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.FileReader;

@Getter
@Slf4j
public class SyncApplication {

    private final static Gson GSON = new GsonBuilder()
            .registerTypeAdapter(AbstractSyncTaskConfiguration.class, new JsonAdapter<>())
            .create();

    private ApplicationConfiguration configuration;
    private ApplicationMailer mailer;
    private SyncTaskScheduler scheduler;

    public void enable() throws Exception {
        try (final JsonReader reader = new JsonReader(new FileReader("config.json"))) {
            configuration = GSON.fromJson(reader, ApplicationConfiguration.class);
        }

        mailer = new ApplicationMailer(this);
        scheduler = new SyncTaskScheduler(this);
    }

    public void disable() throws Exception {
        if (scheduler != null) {
            scheduler.stopTasks();
        }
    }

    public static void main(final String[] args) {
        final SyncApplication application = new SyncApplication();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                application.disable();
            } catch (final Exception e) {
                log.error("An error occurred while disabling application", e);
            }
        }));

        new Thread(() -> {
            try {
                application.enable();
            } catch (final Exception e) {
                log.error("An error occurred while enabling application", e);
                System.exit(1);
            }
        }).start();

        try {
            synchronized (application) {
                application.wait();
            }
        } catch (final InterruptedException e) {
            log.error("An error occurred while interrupting", e);
        }
    }

}
