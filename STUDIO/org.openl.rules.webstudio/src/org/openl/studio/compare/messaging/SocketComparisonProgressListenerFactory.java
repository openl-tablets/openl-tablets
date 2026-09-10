package org.openl.studio.compare.messaging;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import org.openl.studio.projects.messaging.ProjectSocketNotificationService;
import org.openl.studio.projects.service.ExecutionProgressListener;
import org.openl.studio.projects.service.ExecutionStatus;
import org.openl.util.StringUtils;

/**
 * Creates the listener that tells a user how their comparison is going, over the WebSocket.
 */
@RequiredArgsConstructor
@Component
public class SocketComparisonProgressListenerFactory {

    private final ProjectSocketNotificationService socketNotificationService;

    /**
     * Creates a listener that reports the progress of one comparison to the user who started it.
     *
     * @param userName     the user to notify
     * @param comparisonId identifier of the comparison
     * @return the progress listener
     */
    public ExecutionProgressListener create(String userName, String comparisonId) {
        return new ExecutionProgressListener() {
            @Override
            public void onStatusChanged(ExecutionStatus status) {
                socketNotificationService.notifyComparisonStatus(userName, comparisonId, status);
            }

            @Override
            public void onError(String message, Throwable cause) {
                socketNotificationService.notifyComparisonError(userName, comparisonId, reasonOf(message, cause));
            }
        };
    }

    /**
     * What to tell the user went wrong. A failure without a message would leave the screen with
     * nothing to show, so it is named by what it is instead.
     */
    private static String reasonOf(@Nullable String message, @Nullable Throwable cause) {
        if (StringUtils.isNotBlank(message)) {
            return message;
        }
        return cause == null ? ExecutionStatus.ERROR.name() : cause.getClass().getSimpleName();
    }
}
