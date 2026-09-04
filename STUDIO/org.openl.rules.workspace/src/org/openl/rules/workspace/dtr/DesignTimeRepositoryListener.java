package org.openl.rules.workspace.dtr;

import java.util.EventListener;

/**
 * Told when the design repository has been re-read.
 *
 * <p>Two things happen at that moment, and they are not the same. Whatever the repository now holds,
 * everything built on the previous read is stale, so {@link #onRepositoryModified()} runs on every
 * pass. Whether the content itself moved is a separate question: only then does
 * {@link #onRepositoryContentChanged()} follow, for the listeners that have something to say to the
 * sessions showing it.
 */
public interface DesignTimeRepositoryListener extends EventListener {

    /** The repository was re-read; anything cached from the previous read must be dropped. */
    void onRepositoryModified();

    /** The repository holds something other than it did: what the sessions show is out of date. */
    default void onRepositoryContentChanged() {
    }
}
