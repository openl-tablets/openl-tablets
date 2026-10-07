package org.openl.rules.webstudio.notification.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import org.openl.rules.webstudio.notification.event.NotificationEvent;
import org.openl.util.StringUtils;

@Service
public class NotificationServiceImpl implements NotificationService {

    /** Control characters other than line breaks and tabs: they have no place in a message shown as text. */
    private static final String CONTROL_CHARACTERS = "[\\p{Cntrl}&&[^\n\t]]";

    private final Path notificationFile;
    private final ApplicationEventPublisher publisher;

    public NotificationServiceImpl(@Value("${admin.notification-file}") String notificationFile,
                                   ApplicationEventPublisher publisher) {
        this.notificationFile = Path.of(notificationFile);
        this.publisher = publisher;
    }

    @Override
    public String get() throws IOException {
        if (!Files.exists(notificationFile)) {
            return null;
        }
        try (var lines = Files.lines(notificationFile)) {
            return lines.collect(Collectors.joining("\r\n"));
        }
    }

    @Override
    public void send(String notification) throws IOException {
        var message = notification == null ? null : notification.replaceAll(CONTROL_CHARACTERS, "");
        if (StringUtils.isBlank(message)) {
            Files.deleteIfExists(notificationFile);
        } else {
            Files.writeString(notificationFile, message);
        }
        publisher.publishEvent(new NotificationEvent(message, this));
    }
}
