package de.waldorfaugsburg.syncer.module;

import de.waldorfaugsburg.syncer.SyncerApplication;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class ModuleRegistry {

    private final SyncerApplication application;
    private final Map<Class<? extends AbstractModule>, AbstractModule> instanceMap = new HashMap<>();

    public ModuleRegistry(final SyncerApplication application) {
        this.application = application;
    }

    public <T extends AbstractModule> T getOrCreateInstance(final Class<T> moduleClass) {
        final AbstractModule module = instanceMap.get(moduleClass);
        if (module != null) {
            return (T) module;
        }

        try {
            final T moduleInstance = moduleClass.getDeclaredConstructor(SyncerApplication.class).newInstance(application);
            instanceMap.put(moduleClass, moduleInstance);
            log.info("Initialize module instance {}", moduleClass.getSimpleName());
            moduleInstance.init();

            return moduleInstance;
        } catch (final InstantiationException | IllegalAccessException | InvocationTargetException |
                       NoSuchMethodException e) {
            log.info("Error creating module instance {}", moduleClass.getSimpleName(), e);
            throw new RuntimeException(e);
        } catch (final Exception e) {
            log.info("Error initializing module instance {}", moduleClass.getSimpleName(), e);
            throw new RuntimeException(e);
        }
    }

    public <T extends AbstractModule> void destroyInstance(final Class<T> moduleClass) {
        final AbstractModule module = instanceMap.remove(moduleClass);
        if (module == null)
            return;

        try {
            log.info("Destroy module instance {}", moduleClass.getSimpleName());
            module.destroy();
        } catch (final Exception e) {
            log.info("Error destroying module instance {}", moduleClass.getSimpleName(), e);
            throw new RuntimeException(e);
        }
    }
}
