package org.openl.studio.projects.messaging;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
    void sends_one_debounced_broadcast_whatever_the_source() {
        broadcaster.broadcastChanged();

        // One shared key: bursts from every source (commits, locks) collapse into one ping.
        verify(debouncer).debounce(eq("/topic/projects/changed"), any(ChangeNotes.class), any());
        verify(notificationService).notifyProjectsChanged(new ChangeNotes(Set.of(), Set.of()));
    }

    @Test
    void a_commit_made_by_a_request_names_the_client_behind_it() {
        when(changeOrigin.current()).thenReturn("tab-1");

        broadcaster.broadcastChanged();

        // The tab that committed already re-read; every other session still gets the broadcast.
        verify(notificationService).notifyProjectsChanged(new ChangeNotes(Set.of(), Set.of("tab-1")));
    }
}
