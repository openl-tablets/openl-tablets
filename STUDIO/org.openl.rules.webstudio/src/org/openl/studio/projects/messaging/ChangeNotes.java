package org.openl.studio.projects.messaging;

import java.util.Collection;
import java.util.Set;

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

    /** The notes of one change: the files it touched, and the clients it can be attributed to. */
    public static ChangeNotes of(Collection<String> files, Collection<String> origins) {
        return new ChangeNotes(Set.copyOf(files), Set.copyOf(origins));
    }
}
