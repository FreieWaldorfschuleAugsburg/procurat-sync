package de.waldorfaugsburg.syncer.mail;

import com.google.common.base.Preconditions;
import de.waldorfaugsburg.syncer.SyncerApplication;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.simplejavamail.api.email.Email;
import org.simplejavamail.api.mailer.Mailer;
import org.simplejavamail.api.mailer.config.TransportStrategy;
import org.simplejavamail.email.EmailBuilder;
import org.simplejavamail.mailer.MailerBuilder;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

@Slf4j
public final class ApplicationMailer {

    private final SyncerApplication application;
    private final Mailer mailer;

    public ApplicationMailer(final SyncerApplication application) {
        this.application = application;
        this.mailer = MailerBuilder
                .withSMTPServer(
                        application.getConfiguration().getMail().getHost(),
                        application.getConfiguration().getMail().getPort(),
                        application.getConfiguration().getMail().getUsername(),
                        application.getConfiguration().getMail().getPassword())
                .withTransportStrategy(TransportStrategy.SMTP_TLS)
                .buildMailer();
    }

}
