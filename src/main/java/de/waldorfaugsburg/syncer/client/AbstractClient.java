package de.waldorfaugsburg.syncer.client;

public abstract class AbstractClient {

    protected abstract <T extends Exception> void setup() throws T;

    public abstract void close() throws Exception;

}
