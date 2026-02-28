package de.waldorfaugsburg.syncer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import de.waldorfaugsburg.syncer.config.ApplicationConfiguration;
import de.waldorfaugsburg.syncer.mail.ApplicationMailer;
import de.waldorfaugsburg.syncer.module.ModuleRegistry;
import de.waldorfaugsburg.syncer.task.TaskRegistry;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;

import java.io.FileReader;

@Getter
@Slf4j
public class SyncerApplication {

    private final Gson gson = new GsonBuilder().create();

    private ApplicationConfiguration configuration;
    private ApplicationMailer mailer;

    private ModuleRegistry moduleRegistry;
    private TaskRegistry taskRegistry;

    public void enable() throws Exception {
        Logger logger = LogManager.getRootLogger();
        Configurator.setAllLevels(logger.getName(), Level.getLevel("DEBUG"));

        configuration = loadConfiguration("app.json", ApplicationConfiguration.class);
        mailer = new ApplicationMailer(this);

        moduleRegistry = new ModuleRegistry(this);
        taskRegistry = new TaskRegistry(this);
    }

    public void disable() throws Exception {
    }

    public <T> T loadConfiguration(final String fileName, final Class<T> clazz) throws Exception {
        try (final JsonReader reader = new JsonReader(new FileReader(fileName))) {
            return gson.fromJson(reader, clazz);
        }
    }

    public static void main(final String[] args) {
        final SyncerApplication application = new SyncerApplication();

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
