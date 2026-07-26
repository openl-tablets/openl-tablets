package org.openl.studio.projects.messaging;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import jakarta.annotation.PreDestroy;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Coalesces bursts of change notifications into one delivery per key.
 *
 * A compile cycle, a merge or a copy produces a run of identical "something changed" signals within
 * a moment of each other. The first signal of a burst schedules the delivery; every further signal
 * within the window is absorbed by it. The delivery runs after the window on a scheduler thread,
 * never on the caller's.
 */
@Component
@Slf4j
public class NotificationDebouncer {

    private static final long WINDOW_MS = 1000;

    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor(runnable -> {
                var thread = new Thread(runnable, "openl-ws-notification-debouncer");
                thread.setDaemon(true);
                return thread;
            });
    private final Set<String> pending = ConcurrentHashMap.newKeySet();
    private final long windowMs;

    @Autowired
    public NotificationDebouncer() {
        this(WINDOW_MS);
    }

    /** Test seam: a shorter window keeps the burst tests fast. */
    NotificationDebouncer(long windowMs) {
        this.windowMs = windowMs;
    }

    /**
     * Runs the action once per burst of calls sharing the key. The action delivered is the one of the
     * call that opened the window; callers must pass an action that reads current state when it runs.
     *
     * @param key    what the burst is about — a user name, a topic
     * @param action the delivery
     */
    public void debounce(String key, Runnable action) {
        if (pending.add(key)) {
            scheduler.schedule(() -> {
                pending.remove(key);
                try {
                    action.run();
                } catch (RuntimeException e) {
                    log.warn("Failed to deliver a change notification for '{}'.", key, e);
                }
            }, windowMs, TimeUnit.MILLISECONDS);
        }
    }

    @PreDestroy
    void shutdown() {
        scheduler.shutdownNow();
    }
}
