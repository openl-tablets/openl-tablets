package org.openl.rules.webstudio.notification.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import org.openl.rules.webstudio.notification.event.NotificationEvent;

class NotificationServiceImplTest {

    @TempDir
    Path folder;

    private Path file;
    private ApplicationEventPublisher publisher;
    private NotificationServiceImpl service;

    @BeforeEach
    void setUp() {
        file = folder.resolve("notification.txt");
        publisher = mock(ApplicationEventPublisher.class);
        service = new NotificationServiceImpl(file.toString(), publisher);
    }

    @Test
    void storesTheMessageAndAnnouncesIt() throws IOException {
        service.send("Maintenance at 22:00\nExpect a restart");

        assertEquals("Maintenance at 22:00\nExpect a restart", Files.readString(file));
        assertEquals("Maintenance at 22:00\r\nExpect a restart", service.get());
        assertEquals("Maintenance at 22:00\nExpect a restart", lastAnnouncement(1).getMessage());
    }

    @Test
    void dropsControlCharactersButKeepsLineBreaksAndTabs() throws IOException {
        service.send("Line\r\nbreak\u0007\tbell\u0000");

        assertEquals("Line\nbreak\tbell", Files.readString(file));
        assertEquals("Line\nbreak\tbell", lastAnnouncement(1).getMessage());
    }

    @Test
    void aBlankMessageRemovesTheNotification() throws IOException {
        service.send("Old news");

        service.send("  ");

        assertFalse(Files.exists(file));
        assertNull(service.get());
        assertEquals("  ", lastAnnouncement(2).getMessage());
    }

    @Test
    void noMessageRemovesTheNotification() throws IOException {
        service.send("Old news");

        service.send(null);

        assertFalse(Files.exists(file));
        assertNull(lastAnnouncement(2).getMessage());
    }

    @Test
    void thereIsNoNotificationUntilOneIsSent() throws IOException {
        assertNull(service.get());
    }

    private NotificationEvent lastAnnouncement(int announcements) {
        var events = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(publisher, times(announcements)).publishEvent(events.capture());
        return events.getAllValues().getLast();
    }
}
