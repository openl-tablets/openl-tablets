package org.openl.rules.webstudio.notification.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import org.openl.rules.webstudio.notification.event.NotificationEvent;
import org.openl.util.StringUtils;

@Component
public class NotificationServiceImpl implements NotificationService {

    private final Path NOTIFICATION_FILE;
    private final ApplicationEventPublisher publisher;

    public NotificationServiceImpl(@Value("${admin.notification-file}") String notificationFile,
                                   ApplicationEventPublisher publisher) {
        this.NOTIFICATION_FILE = Paths.get(notificationFile);
        this.publisher = publisher;
    }

    @Override
    public String get() throws IOException {
        if (!Files.exists(NOTIFICATION_FILE)) {
            return null;
        }
        try (Stream<String> lines = Files.lines(NOTIFICATION_FILE)) {
            return lines.collect(Collectors.joining("\r\n"));
        }
    }

    @Override
    public void send(String notification) throws IOException {
        if (StringUtils.isBlank(notification)) {
            Files.deleteIfExists(NOTIFICATION_FILE);
        } else {
            Files.writeString(NOTIFICATION_FILE, notification);
        }
        publisher.publishEvent(new NotificationEvent(notification, this));
    }
}
