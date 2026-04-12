package de.waldorfaugsburg.syncer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import de.waldorfaugsburg.syncer.config.ApplicationConfiguration;
import de.waldorfaugsburg.syncer.mail.ApplicationMailer;
import de.waldorfaugsburg.syncer.module.ModuleRegistry;
import de.waldorfaugsburg.syncer.module.ews.EWSModule;
import de.waldorfaugsburg.syncer.task.TaskRegistry;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;

import java.io.FileReader;
import java.util.Scanner;

@Getter
@Slf4j
public class SyncerApplication {

    private final Gson gson = new GsonBuilder().create();

    private ApplicationConfiguration configuration;
    private ApplicationMailer mailer;

    private ModuleRegistry moduleRegistry;
    private TaskRegistry taskRegistry;

    public void enable(final String[] args) throws Exception {
        configuration = loadConfiguration("app.json", ApplicationConfiguration.class);
        mailer = new ApplicationMailer(this);

        moduleRegistry = new ModuleRegistry(this);
        taskRegistry = new TaskRegistry(this);

        if (configuration.isPretendMode()) {
            log.warn("PRETEND MODE is active");
        }

        if (args.length == 1) {
            taskRegistry.invokeTask(args[0]);
        }
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
                application.enable(args);
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
