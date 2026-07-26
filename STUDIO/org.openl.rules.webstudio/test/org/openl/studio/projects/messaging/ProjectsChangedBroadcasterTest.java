package org.openl.studio.projects.messaging;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;

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

    @InjectMocks
    private ProjectsChangedBroadcaster broadcaster;

    @Test
    void sends_one_debounced_broadcast_whatever_the_source() {
        doAnswer(invocation -> {
            invocation.getArgument(1, Runnable.class).run();
            return null;
        }).when(debouncer).debounce(any(), any());

        broadcaster.broadcastChanged();

        // One shared key: bursts from every source (commits, locks) collapse into one ping.
        verify(debouncer).debounce(eq("/topic/projects/changed"), any());
        verify(notificationService).notifyProjectsChanged();
    }
}
