package org.openl.studio.projects.messaging;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import jakarta.annotation.Nullable;

/**
 * What a change ping stands for: the files the change touched, and the clients whose requests made it.
 *
 * <p>A debounce window merges the notes of everything it absorbed, so one ping can stand for the
 * changes of several clients. A client may read a ping as the echo of its own action only when it is
 * the sole origin — a single origin would silently drop another session's change hiding behind it.
 *
 * <p>A change made outside a request — the workspace files watcher, a repository poll — names no
 * origin, and no client can mistake it for its own.
 */
public record ChangeNotes(Set<String> files, Set<String> origins) {

    /** The notes of one change: the files it touched, and the client that asked for it, if any. */
    public static ChangeNotes of(Collection<String> files, @Nullable String origin) {
        return new ChangeNotes(Set.copyOf(files), origin == null ? Set.of() : Set.of(origin));
    }

    /** The notes of two changes as one. */
    public ChangeNotes merge(ChangeNotes other) {
        return new ChangeNotes(union(files, other.files), union(origins, other.origins));
    }

    private static Set<String> union(Set<String> first, Set<String> second) {
        if (first.isEmpty() || second.isEmpty()) {
            return first.isEmpty() ? second : first;
        }
        var merged = new LinkedHashSet<>(first);
        merged.addAll(second);
        return Set.copyOf(merged);
    }
}
