package org.openl.studio.projects.messaging;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
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

    /** The open windows and what they have gathered; guarded by {@code lock}. */
    private final Map<String, Window> pending = new HashMap<>();
    private final Object lock = new Object();

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
     * Runs the action once per burst of calls sharing the key. The notes of a burst merge, and the
     * delivery receives everything the window gathered — the files the changes touched and the
     * clients that asked for them.
     *
     * <p>The notes are drained atomically with the window's close, so a signal arriving during a
     * delivery opens a new window carrying its own notes — nothing is swept into the finished
     * delivery or lost. The action delivered is the one of the call that opened the window; callers
     * must pass an action that reads current state when it runs.
     *
     * @param key    what the burst is about — a user name, a topic
     * @param notes  what this signal adds; may be empty
     * @param action the delivery, receiving the merged notes of the burst
     */
    public void debounce(String key, ChangeNotes notes, Consumer<ChangeNotes> action) {
        synchronized (lock) {
            var open = pending.get(key);
            if (open != null) {
                open.absorb(notes);
                return;
            }
            pending.put(key, new Window(notes));
        }
        scheduler.schedule(() -> deliver(key, action), windowMs, TimeUnit.MILLISECONDS);
    }

    private void deliver(String key, Consumer<ChangeNotes> action) {
        Window window;
        synchronized (lock) {
            window = pending.remove(key);
        }
        try {
            action.accept(window.drain());
        } catch (RuntimeException e) {
            log.warn("Failed to deliver a change notification for '{}'.", key, e);
        }
    }

    /**
     * What one open window has gathered. The notes are merged in place, so a burst of a thousand
     * file events costs one pass over them rather than a rebuilt set per event — and the lock the
     * merge holds is the one every key shares.
     *
     * <p>A change nobody can be named for takes the whole window's names with it. One ping then
     * stands for that change as well, and a client that read its own name on it would skip a change
     * it did not make.
     */
    private static final class Window {

        private final Set<String> files = new LinkedHashSet<>();
        private final Set<String> origins = new LinkedHashSet<>();
        private boolean anonymous;

        private Window(ChangeNotes notes) {
            absorb(notes);
        }

        private void absorb(ChangeNotes notes) {
            files.addAll(notes.files());
            anonymous |= notes.origins().isEmpty();
            origins.addAll(notes.origins());
        }

        private ChangeNotes drain() {
            return ChangeNotes.of(files, anonymous ? Set.of() : origins);
        }
    }

    @PreDestroy
    void shutdown() {
        scheduler.shutdownNow();
    }
}
