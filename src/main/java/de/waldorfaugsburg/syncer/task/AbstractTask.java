package de.waldorfaugsburg.syncer.task;

import de.waldorfaugsburg.syncer.SyncerApplication;
import de.waldorfaugsburg.syncer.module.AbstractModule;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;

@Slf4j
public abstract class AbstractTask<C> {

    private final SyncerApplication application;
    private final Set<Class<? extends AbstractModule>> modules;
    private final String configurationPath;
    private final Class<C> configurationClass;

    @Getter
    private C configuration;

    private boolean running;

    @SafeVarargs
    public AbstractTask(final SyncerApplication application,
                        final String configurationPath,
                        final Class<C> configurationClass,
                        final Class<? extends AbstractModule>... modules) {
        this.application = application;
        this.configurationPath = configurationPath;
        this.configurationClass = configurationClass;
        this.modules = Set.of(modules);
    }

    public void preRun() throws Exception {
        loadModules();
        loadConfiguration();
    }

    public abstract void run() throws Exception;

    public void postRun() throws Exception {
    }

    public void loadConfiguration() throws Exception {
        configuration = application.loadConfiguration(configurationPath, configurationClass);
    }

    void invoke() {
        if (running) {
            throw new IllegalStateException("already running");
        }

        try {
            log.info("Task {} pre-run", getClass().getSimpleName());
            preRun();
            running = true;

            log.info("Task {} run", getClass().getSimpleName());
            run();

            running = false;
            log.info("Task {} post-run", getClass().getSimpleName());
            postRun();
        } catch (final Exception e) {
            log.error("Error while running task", e);
        } finally {
            destroyModules();
        }
    }

    private void loadModules() {
        modules.forEach(module -> application.getModuleRegistry().getOrCreateInstance(module));
    }

    private void destroyModules() {
        modules.forEach(module -> application.getModuleRegistry().destroyInstance(module));
    }

    protected SyncerApplication getApplication() {
        return application;
    }
}
