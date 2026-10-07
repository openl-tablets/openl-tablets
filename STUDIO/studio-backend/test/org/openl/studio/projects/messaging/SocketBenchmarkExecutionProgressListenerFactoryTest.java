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
import org.openl.studio.projects.service.ExecutionStatus;

@ExtendWith(MockitoExtension.class)
class SocketBenchmarkExecutionProgressListenerFactoryTest {

    private static final String TABLE_ID = "abc123";

    @Mock
    private ProjectSocketNotificationService notificationService;

    @Mock
    private CommonUser user;

    private SocketBenchmarkExecutionProgressListenerFactory factory;
    private ProjectIdModel projectId;

    @BeforeEach
    void setUp() {
        factory = new SocketBenchmarkExecutionProgressListenerFactory(notificationService);
        projectId = ProjectIdModel.builder()
                .repository("design")
                .projectName("TestProject")
                .build();
    }

    @Test
    void reportsEveryStatusOfTheMeasurement() {
        var listener = factory.create(user, projectId, TABLE_ID);

        listener.onStatusChanged(ExecutionStatus.PENDING);
        listener.onStatusChanged(ExecutionStatus.STARTED);
        listener.onStatusChanged(ExecutionStatus.COMPLETED);

        verify(notificationService).notifyBenchmarkExecutionStatus(user, projectId, TABLE_ID, ExecutionStatus.PENDING);
        verify(notificationService).notifyBenchmarkExecutionStatus(user, projectId, TABLE_ID, ExecutionStatus.STARTED);
        verify(notificationService).notifyBenchmarkExecutionStatus(user, projectId, TABLE_ID, ExecutionStatus.COMPLETED);
        verifyNoMoreInteractions(notificationService);
    }

    @Test
    void reportsWhyTheMeasurementFailed() {
        var listener = factory.create(user, projectId, TABLE_ID);

        listener.onError("Something failed", new RuntimeException("test error"));

        verify(notificationService).notifyBenchmarkExecutionError(user, projectId, TABLE_ID, "Something failed");
        verifyNoMoreInteractions(notificationService);
    }
}
