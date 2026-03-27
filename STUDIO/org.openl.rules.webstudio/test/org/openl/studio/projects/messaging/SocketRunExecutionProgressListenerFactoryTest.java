package org.openl.studio.projects.messaging;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.openl.rules.common.CommonUser;
import org.openl.studio.projects.model.ProjectIdModel;
import org.openl.studio.projects.service.ExecutionProgressListener;
import org.openl.studio.projects.service.ExecutionStatus;

@ExtendWith(MockitoExtension.class)
class SocketRunExecutionProgressListenerFactoryTest {

    private static final String TABLE_ID = "abc123";

    @Mock
    private ProjectSocketNotificationService notificationService;

    @Mock
    private CommonUser user;

    private SocketRunExecutionProgressListenerFactory factory;
    private ProjectIdModel projectId;

    @BeforeEach
    void setUp() {
        factory = new SocketRunExecutionProgressListenerFactory(notificationService);
        projectId = ProjectIdModel.builder()
                .repository("design")
                .projectName("TestProject")
                .build();
    }

    @Test
    void onStatusChanged_pending() {
        ExecutionProgressListener listener = factory.create(user, projectId, TABLE_ID);

        listener.onStatusChanged(ExecutionStatus.PENDING);

        verify(notificationService).notifyRunExecutionStatus(user, projectId, TABLE_ID, ExecutionStatus.PENDING);
        verifyNoMoreInteractions(notificationService);
    }

    @Test
    void onStatusChanged_started() {
        ExecutionProgressListener listener = factory.create(user, projectId, TABLE_ID);

        listener.onStatusChanged(ExecutionStatus.STARTED);

        verify(notificationService).notifyRunExecutionStatus(user, projectId, TABLE_ID, ExecutionStatus.STARTED);
    }

    @Test
    void onStatusChanged_completed() {
        ExecutionProgressListener listener = factory.create(user, projectId, TABLE_ID);

        listener.onStatusChanged(ExecutionStatus.COMPLETED);

        verify(notificationService).notifyRunExecutionStatus(user, projectId, TABLE_ID, ExecutionStatus.COMPLETED);
    }

    @Test
    void onError() {
        ExecutionProgressListener listener = factory.create(user, projectId, TABLE_ID);
        var cause = new RuntimeException("test error");

        listener.onError("Something failed", cause);

        verify(notificationService).notifyRunExecutionError(user, projectId, TABLE_ID, "Something failed");
        verifyNoMoreInteractions(notificationService);
    }

    @Test
    void onError_nullCause() {
        ExecutionProgressListener listener = factory.create(user, projectId, TABLE_ID);

        listener.onError("Null cause error", null);

        verify(notificationService).notifyRunExecutionError(user, projectId, TABLE_ID, "Null cause error");
    }

    @Test
    void multipleStatusChanges() {
        ExecutionProgressListener listener = factory.create(user, projectId, TABLE_ID);

        listener.onStatusChanged(ExecutionStatus.PENDING);
        listener.onStatusChanged(ExecutionStatus.STARTED);
        listener.onStatusChanged(ExecutionStatus.COMPLETED);

        verify(notificationService).notifyRunExecutionStatus(user, projectId, TABLE_ID, ExecutionStatus.PENDING);
        verify(notificationService).notifyRunExecutionStatus(user, projectId, TABLE_ID, ExecutionStatus.STARTED);
        verify(notificationService).notifyRunExecutionStatus(user, projectId, TABLE_ID, ExecutionStatus.COMPLETED);
        verifyNoMoreInteractions(notificationService);
    }
}
