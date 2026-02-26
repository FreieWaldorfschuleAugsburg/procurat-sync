package de.waldorfaugsburg.syncer.module;

import de.waldorfaugsburg.syncer.SyncerApplication;

public abstract class AbstractModule {

    private final SyncerApplication application;

    protected AbstractModule(final SyncerApplication application) {
        this.application = application;
    }

    public abstract void init() throws Exception;

    public abstract void destroy() throws Exception;

    protected SyncerApplication getApplication() {
        return application;
    }
}
