package org.openl.studio.projects.messaging;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * The one debounced sender of the {@code /user/topic/projects/changed} ping every user receives.
 *
 * Every source of an everyone-visible change — a design repository commit, a project lock appearing
 * or releasing — reports here, and a burst from any mix of them collapses into one ping. The ping
 * goes to each connected user separately, naming the clients of that user that were writing just
 * before — the session that committed then recognises its own echo, while a name never travels to
 * anyone who could not have set it.
 */
@Component
@RequiredArgsConstructor
public class ProjectsChangedBroadcaster {

    private static final String DEBOUNCE_KEY = "/topic/projects/changed";

    private final ProjectSocketNotificationService notificationService;
    private final NotificationDebouncer debouncer;
    private final ChangeOriginResolver changeOrigin;

    public void broadcastChanged() {
        // Read when the change is reported, not when the window drains: by then the clients that
        // were writing may have fallen out of the recent window.
        var writers = changeOrigin.recentWritersByUser();
        debouncer.debounce(DEBOUNCE_KEY, ChangeNotes.of(List.of(), List.of()),
                notes -> notificationService.notifyProjectsChanged(writers));
    }
}
