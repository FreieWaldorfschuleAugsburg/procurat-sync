package de.waldorfaugsburg.sync.module;

import de.waldorfaugsburg.sync.SyncApplication;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class ModuleRegistry {

    private final SyncApplication application;
    private final Map<Class<?>, AbstractModule> instanceMap = new HashMap<>();

    public ModuleRegistry(final SyncApplication application) {
        this.application = application;
    }

    public <T extends AbstractModule> T createModuleInstance(final Class<T> moduleClass) {
        final AbstractModule module = instanceMap.get(moduleClass);
        if (module != null) {
            return (T) module;
        }

        try {
            final T moduleInstance = moduleClass.getConstructor(SyncApplication.class).newInstance(application);
            instanceMap.put(moduleClass, moduleInstance);
            return moduleInstance;
        } catch (final InstantiationException | IllegalAccessException | InvocationTargetException |
                 NoSuchMethodException e) {
            log.info("Error creating module instance", e);
            throw new RuntimeException(e);
        }
    }

    public <T extends AbstractModule> void destroyModuleInstance(final Class<T> moduleClass) {
        final AbstractModule module = instanceMap.remove(moduleClass);
        try {
            module.close();
        } catch (final IOException e) {
            log.info("Error destroying module instance", e);
            throw new RuntimeException(e);
        }
    }
}
