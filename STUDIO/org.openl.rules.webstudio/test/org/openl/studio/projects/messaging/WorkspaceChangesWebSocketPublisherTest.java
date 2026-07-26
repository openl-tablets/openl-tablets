package org.openl.studio.projects.messaging;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.openl.rules.project.abstraction.RulesProject;
import org.openl.studio.projects.service.ProjectStateChangedEvent;

@ExtendWith(MockitoExtension.class)
class WorkspaceChangesWebSocketPublisherTest {

    @Mock
    private ProjectSocketNotificationService notificationService;

    @Mock
    private NotificationDebouncer debouncer;

    @Mock
    private RulesProject project;

    @InjectMocks
    private WorkspaceChangesWebSocketPublisher publisher;

    @BeforeEach
    void setUp() {
        // The debouncer is a pass-through here; its coalescing has its own test.
        lenient().doAnswer(invocation -> {
            invocation.getArgument(1, Runnable.class).run();
            return null;
        }).when(debouncer).debounce(any(), any());
    }

    @Test
    void pings_the_workspace_of_the_acting_user_on_a_state_change() {
        publisher.onProjectStateChanged(new ProjectStateChangedEvent(project, "jane"));

        verify(notificationService).notifyWorkspaceChanged("jane");
        verify(debouncer).debounce(eq("jane"), any());
    }
}
