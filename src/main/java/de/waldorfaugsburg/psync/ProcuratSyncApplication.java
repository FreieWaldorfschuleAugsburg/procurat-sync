package de.waldorfaugsburg.psync;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.bridge.SLF4JBridgeHandler;

@Slf4j
public class ProcuratSyncApplication {

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
