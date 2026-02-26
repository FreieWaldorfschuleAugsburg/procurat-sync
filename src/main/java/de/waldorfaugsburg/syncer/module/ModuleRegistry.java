package de.waldorfaugsburg.syncer.module;

import de.waldorfaugsburg.syncer.SyncerApplication;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class ModuleRegistry {

    private final SyncerApplication application;
    private final Map<Class<?>, AbstractModule> instanceMap = new HashMap<>();

    public ModuleRegistry(final SyncerApplication application) {
        this.application = application;
    }

    public <T extends AbstractModule> T getOrCreateInstance(final Class<T> moduleClass) {
        final AbstractModule module = instanceMap.get(moduleClass);
        if (module != null) {
            return (T) module;
        }

        try {
            final T moduleInstance = moduleClass.getConstructor(SyncerApplication.class).newInstance(application);
            instanceMap.put(moduleClass, moduleInstance);
            moduleInstance.init();

            return moduleInstance;
        } catch (final InstantiationException | IllegalAccessException | InvocationTargetException |
                       NoSuchMethodException e) {
            log.info("Error creating module instance", e);
            throw new RuntimeException(e);
        } catch (final Exception e) {
            log.info("Error initializing module instance", e);
            throw new RuntimeException(e);
        }
    }

    public <T extends AbstractModule> void destroyInstance(final Class<T> moduleClass) {
        final AbstractModule module = instanceMap.remove(moduleClass);
        try {
            module.destroy();
        } catch (final Exception e) {
            log.info("Error destroying module instance", e);
            throw new RuntimeException(e);
        }
    }
}
