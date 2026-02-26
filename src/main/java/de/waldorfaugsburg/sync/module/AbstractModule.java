package de.waldorfaugsburg.sync.module;

import java.io.Closeable;

public abstract class AbstractModule implements Closeable {

    public abstract void init() throws Exception;

}
