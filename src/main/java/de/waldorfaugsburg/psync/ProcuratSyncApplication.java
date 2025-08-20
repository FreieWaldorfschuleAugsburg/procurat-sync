package de.waldorfaugsburg.psync;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import de.waldorfaugsburg.psync.client.activedirectory.ADClient;
import de.waldorfaugsburg.psync.client.ews.EWSClient;
import de.waldorfaugsburg.psync.client.procurat.ProcuratClient;
import de.waldorfaugsburg.psync.client.starface.StarfaceClient;
import de.waldorfaugsburg.psync.config.ApplicationConfiguration;
import de.waldorfaugsburg.psync.config.JsonAdapter;
import de.waldorfaugsburg.psync.mail.ApplicationMailer;
import de.waldorfaugsburg.psync.task.AbstractSyncTaskConfiguration;
import de.waldorfaugsburg.psync.task.SyncTaskScheduler;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.FileReader;

@Getter
@Slf4j
public class ProcuratSyncApplication {

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
        final ProcuratSyncApplication application = new ProcuratSyncApplication();

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
