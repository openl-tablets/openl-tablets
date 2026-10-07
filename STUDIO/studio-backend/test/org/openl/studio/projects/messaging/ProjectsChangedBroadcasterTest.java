package org.openl.studio.projects.messaging;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProjectsChangedBroadcasterTest {

    @Mock
    private ProjectSocketNotificationService notificationService;

    @Mock
    private NotificationDebouncer debouncer;

    @Mock
    private ChangeOriginResolver changeOrigin;

    @InjectMocks
    private ProjectsChangedBroadcaster broadcaster;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        doAnswer(invocation -> {
            invocation.getArgument(2, Consumer.class).accept(invocation.getArgument(1, ChangeNotes.class));
            return null;
        }).when(debouncer).debounce(any(), any(ChangeNotes.class), any());
    }

    @Test
    void sends_one_ping_per_burst_naming_the_clients_that_were_writing() {
        when(changeOrigin.recentWritersByUser()).thenReturn(Map.of("jane", Set.of("tab-1")));

        broadcaster.broadcastChanged();

        // One shared key: bursts from every source (commits, locks) collapse into one ping, and it
        // tells each user about clients of their own.
        verify(debouncer).debounce(eq("/topic/projects/changed"), any(ChangeNotes.class), any());
        verify(notificationService).notifyProjectsChanged(Map.of("jane", Set.of("tab-1")));
    }
}
