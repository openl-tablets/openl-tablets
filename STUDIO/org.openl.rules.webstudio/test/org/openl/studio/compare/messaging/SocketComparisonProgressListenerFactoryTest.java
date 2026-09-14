package org.openl.studio.compare.messaging;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.openl.studio.projects.messaging.ProjectSocketNotificationService;
import org.openl.studio.projects.service.ExecutionStatus;

@ExtendWith(MockitoExtension.class)
class SocketComparisonProgressListenerFactoryTest {

    private static final String USER = "jsmith";
    private static final String COMPARISON_ID = "cmp-1";

    @Mock
    private ProjectSocketNotificationService notificationService;

    private SocketComparisonProgressListenerFactory factory;

    @BeforeEach
    void setUp() {
        factory = new SocketComparisonProgressListenerFactory(notificationService);
    }

    @Test
    void tellsTheUserEveryStatusTheComparisonPassesThrough() {
        var listener = factory.create(USER, COMPARISON_ID);

        listener.onStatusChanged(ExecutionStatus.STARTED);
        listener.onStatusChanged(ExecutionStatus.COMPLETED);

        verify(notificationService).notifyComparisonStatus(USER, COMPARISON_ID, ExecutionStatus.STARTED);
        verify(notificationService).notifyComparisonStatus(USER, COMPARISON_ID, ExecutionStatus.COMPLETED);
        verifyNoMoreInteractions(notificationService);
    }

    @Test
    void tellsTheUserWhyTheComparisonFailed() {
        var listener = factory.create(USER, COMPARISON_ID);

        listener.onError("Cannot read the file", new IllegalStateException("Cannot read the file"));

        verify(notificationService).notifyComparisonError(USER, COMPARISON_ID, "Cannot read the file");
        verifyNoMoreInteractions(notificationService);
    }

    @Test
    void namesAFailureThatCameWithoutAMessage() {
        var listener = factory.create(USER, COMPARISON_ID);

        listener.onError("  ", new IllegalStateException());

        verify(notificationService).notifyComparisonError(USER, COMPARISON_ID, "IllegalStateException");
    }

    @Test
    void namesAFailureWithNeitherMessageNorCause() {
        var listener = factory.create(USER, COMPARISON_ID);

        listener.onError(null, null);

        verify(notificationService).notifyComparisonError(USER, COMPARISON_ID, "ERROR");
    }
}
