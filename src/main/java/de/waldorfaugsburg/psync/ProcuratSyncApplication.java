package de.waldorfaugsburg.psync;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import de.waldorfaugsburg.psync.client.procurat.ProcuratClient;
import de.waldorfaugsburg.psync.client.starface.StarfaceClient;
import de.waldorfaugsburg.psync.client.starface.model.StarfaceContactTag;
import de.waldorfaugsburg.psync.config.ApplicationConfiguration;
import de.waldorfaugsburg.psync.task.SyncTaskScheduler;
import de.waldorfaugsburg.psync.task.starface.StarfaceSyncTask;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.bridge.SLF4JBridgeHandler;

import java.io.FileReader;
import java.util.List;

@Getter
@Slf4j
public class ProcuratSyncApplication {

    private ApplicationConfiguration configuration;
    private ProcuratClient procuratClient;
    private StarfaceClient starfaceClient;
    private SyncTaskScheduler scheduler;

    public void enable() throws Exception {
        try (final JsonReader reader = new JsonReader(new FileReader("config.json"))) {
            configuration = new Gson().fromJson(reader, ApplicationConfiguration.class);
        }

        procuratClient = new ProcuratClient(configuration.getClients().getProcurat().getUrl(), configuration.getClients().getProcurat().getApiKey(), configuration.getClients().getProcurat().getRootGroupId());
        starfaceClient = new StarfaceClient(configuration.getClients().getStarface().getUrl(), configuration.getClients().getStarface().getUserId(), configuration.getClients().getStarface().getPassword(), configuration.getClients().getStarface().getTag());

        scheduler = new SyncTaskScheduler(this);
    }

    public void disable() throws Exception {
        scheduler.stopTasks();
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

        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();

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
