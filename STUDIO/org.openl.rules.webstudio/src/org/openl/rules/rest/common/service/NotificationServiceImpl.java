package org.openl.rules.rest.common.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import org.openl.util.StringUtils;

@Component
public class NotificationServiceImpl implements NotificationService {

    private final Path NOTIFICATION_FILE;
    private final SimpMessagingTemplate messagingTemplate;

    public NotificationServiceImpl(@Value("${admin.notification-file}") String notificationFile,
                                  SimpMessagingTemplate messagingTemplate) {
        this.NOTIFICATION_FILE = Paths.get(notificationFile);
        this.messagingTemplate = messagingTemplate;
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
        notifyAll(notification);
    }

    private void notifyAll(String message) {
        messagingTemplate.convertAndSend("/topic/public/notification.txt", message == null ? "" : message);
    }
}
