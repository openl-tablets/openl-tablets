package org.openl.studio.projects.messaging;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.openl.rules.project.abstraction.RulesProject;
import org.openl.studio.projects.model.ProjectIdModel;
import org.openl.studio.projects.service.ProjectIdentifierMapper;
import org.openl.studio.projects.service.ProjectStateChangedEvent;

@ExtendWith(MockitoExtension.class)
class WorkspaceChangesWebSocketPublisherTest {

    @Mock
    private ProjectSocketNotificationService notificationService;

    @Mock
    private ProjectIdentifierMapper projectIdentifierMapper;

    @Mock
    private NotificationDebouncer debouncer;

    @Mock
    private RulesProject project;

    @InjectMocks
    private WorkspaceChangesWebSocketPublisher publisher;

    private final ProjectIdModel projectId = ProjectIdModel.builder()
            .repository("design")
            .projectName("Alpha")
            .build();

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        // The debouncer is a pass-through here; its coalescing has its own test. Lenient: not every
        // test drives both destinations through it.
        lenient().doAnswer(invocation -> {
            invocation.getArgument(1, Runnable.class).run();
            return null;
        }).when(debouncer).debounce(any(), any(Runnable.class));
        lenient().doAnswer(invocation -> {
            var payload = new LinkedHashSet<String>(invocation.getArgument(1, Collection.class));
            invocation.getArgument(2, Consumer.class).accept(payload);
            return null;
        }).when(debouncer).debounce(any(), anyCollection(), any());
        lenient().when(projectIdentifierMapper.map(project)).thenReturn(projectId);
    }

    @Test
    void pings_the_workspace_and_the_project_of_the_acting_user_on_a_state_change() {
        var event = new ProjectStateChangedEvent(project, "jane");

        publisher.onProjectStateChanged(event);

        verify(notificationService).notifyWorkspaceChanged("jane");
        verify(notificationService).notifyProjectChanged("jane", projectId, Set.of());
        // Workspace and per-project pings coalesce independently of each other.
        verify(debouncer).debounce(eq("jane"), any(Runnable.class));
        verify(debouncer).debounce(eq("jane|" + projectId.encode()), anyCollection(), any());
    }

    @Test
    void the_files_of_an_edit_ride_the_project_ping() {
        publisher.onProjectStateChanged(new ProjectStateChangedEvent(project, "jane", List.of("rules/A.xlsx")));

        // Merging the files of a burst is the debouncer's own concern, tested with it.
        verify(notificationService).notifyProjectChanged("jane", projectId, Set.of("rules/A.xlsx"));
    }

    @Test
    void the_workspace_ping_survives_a_project_that_cannot_be_identified() {
        var event = new ProjectStateChangedEvent(project, "jane");
        when(projectIdentifierMapper.map(project)).thenThrow(new IllegalStateException("gone"));

        publisher.onProjectStateChanged(event);

        verify(notificationService).notifyWorkspaceChanged("jane");
    }
}
