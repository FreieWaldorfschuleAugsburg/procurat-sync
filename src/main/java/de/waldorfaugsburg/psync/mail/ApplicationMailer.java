package de.waldorfaugsburg.psync.mail;

import com.google.common.base.Preconditions;
import de.waldorfaugsburg.psync.ProcuratSyncApplication;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.simplejavamail.api.email.Email;
import org.simplejavamail.api.mailer.Mailer;
import org.simplejavamail.api.mailer.config.TransportStrategy;
import org.simplejavamail.email.EmailBuilder;
import org.simplejavamail.mailer.MailerBuilder;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public final class ApplicationMailer {

    private final ProcuratSyncApplication application;
    private final Mailer mailer;

    private final Map<String, MailTemplate> templateMap = new HashMap<>();

    public ApplicationMailer(final ProcuratSyncApplication application) {
        this.application = application;
        this.mailer = MailerBuilder
                .withSMTPServer(
                        application.getConfiguration().getMail().getHost(),
                        application.getConfiguration().getMail().getPort(),
                        application.getConfiguration().getMail().getUsername(),
                        application.getConfiguration().getMail().getPassword())
                .withTransportStrategy(TransportStrategy.SMTP_TLS)
                .buildMailer();

        loadTemplates();
    }

    private void loadTemplates() {
        try {
            final String jarPath;
            jarPath = "jar:file:" + getClass().getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .toURI()
                    .getPath();

            try (final FileSystem fileSystem = FileSystems.newFileSystem(URI.create(jarPath), Collections.emptyMap())) {
                final List<Path> templates;
                try (final Stream<Path> stream = Files.walk(fileSystem.getPath("mailTemplates/"))) {
                    templates = stream.filter(Files::isRegularFile).toList();
                }

                for (final Path path : templates) {
                    try {
                        final String name = FilenameUtils.removeExtension(path.getFileName().toString());
                        final String content = Files.readString(path);
                        final Document document = Jsoup.parse(content);
                        templateMap.put(name, new MailTemplate(name, document.title(), document.body().html()));
                        log.info("Registered mail template {}", name);
                    } catch (final IOException e) {
                        log.error("Error while reading mail template '{}'", path, e);
                    }
                }
            }
        } catch (final IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public void sendMail(final String templateName, final String... replacements) {
        final MailTemplate mailTemplate = templateMap.get(templateName);
        if (mailTemplate == null) {
            throw new IllegalArgumentException("Invalid template: " + templateName);
        }

        sendMail(application.getConfiguration().getMail().getRecipients(), mailTemplate, replacements);
    }

    public void sendMail(final List<String> recipients, final MailTemplate template, final String... replacements) {
        Preconditions.checkNotNull(recipients, "recipients may not be null");
        Preconditions.checkNotNull(template, "template may not be null");
        Preconditions.checkNotNull(replacements, "replacements may not be null");

        // Handling parameters
        String subject = applyReplacements(template.subject(), replacements);
        String content = template.content();
        Preconditions.checkNotNull(content, "content may not be null");

        content = applyReplacements(content, replacements);

        // Sending mail
        final Email email = EmailBuilder.startingBlank()
                .toMultiple(recipients)
                .from(application.getConfiguration().getMail().getUsername())
                .withSubject(subject)
                .withHTMLText(content)
                .buildEmail();
        mailer.sendMail(email, true).whenComplete((it, throwable) -> {
            if (throwable != null) {
                log.error("An error occurred while sending mail with template '{}' to '{}'", template.name(), recipients, throwable);
                return;
            }

            log.info("Mail with template '{}' successfully sent to '{}'", template.name(), recipients);
        });
    }

    private String applyReplacements(String initialString, final String... replacements) {
        for (int i = 0; i < replacements.length; i += 2) {
            initialString = initialString.replace(replacements[i], replacements[i + 1]);
        }
        return initialString;
    }

}
