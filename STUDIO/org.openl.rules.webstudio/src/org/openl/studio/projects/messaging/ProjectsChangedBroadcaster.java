package org.openl.studio.projects.messaging;

import java.util.Set;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * The one debounced sender of the {@code /topic/projects/changed} broadcast.
 *
 * Every source of an everyone-visible change — a design repository commit, a project lock appearing
 * or releasing — reports here, and a burst from any mix of them collapses into one ping. The ping
 * names the clients whose requests caused it, so the tab that committed recognises its own echo.
 */
@Component
@RequiredArgsConstructor
public class ProjectsChangedBroadcaster {

    private static final String DEBOUNCE_KEY = "/topic/projects/changed";

    private final ProjectSocketNotificationService notificationService;
    private final NotificationDebouncer debouncer;
    private final ChangeOriginResolver changeOrigin;

    public void broadcastChanged() {
        debouncer.debounce(DEBOUNCE_KEY, ChangeNotes.of(Set.of(), changeOrigin.current()),
                notificationService::notifyProjectsChanged);
    }
}
