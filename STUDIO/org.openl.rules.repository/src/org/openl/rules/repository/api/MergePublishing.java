package org.openl.rules.repository.api;

import java.io.IOException;

import org.jspecify.annotations.Nullable;

/**
 * Tells the code that stops a merge by interrupting its thread when the merge can no longer be stopped.
 *
 * <p>A merge that has started to publish its result finishes: a push cut off half way may have reached the remote
 * repository already, while the merge would be undone locally and reported as stopped. So a merge announces the
 * moment it starts to publish, and the code that runs it no longer interrupts its thread from then on.
 *
 * <p>The announcement reaches the listener of the thread the merge runs on. A merge run without one announces to
 * nobody.
 */
public final class MergePublishing {

    private static final ThreadLocal<@Nullable Runnable> LISTENER = new ThreadLocal<>();

    private MergePublishing() {
    }

    /**
     * Runs a merge, telling the listener when the merge starts to publish its result.
     *
     * @param listener told on the thread of the merge, right before the merge is published
     * @param merge    the merge
     * @param <T>      what the merge returns
     * @return what the merge returns
     * @throws IOException when the merge fails
     */
    public static <T> T listen(Runnable listener, Merge<T> merge) throws IOException {
        LISTENER.set(listener);
        try {
            return merge.run();
        } finally {
            LISTENER.remove();
        }
    }

    /**
     * Announces that the merge running on this thread starts to publish its result, and so finishes whatever happens
     * from now on.
     */
    public static void announce() {
        var listener = LISTENER.get();
        if (listener != null) {
            listener.run();
        }
    }

    /**
     * A merge, which may fail.
     *
     * @param <T> what it returns
     */
    @FunctionalInterface
    public interface Merge<T> {

        /**
         * Runs the merge.
         *
         * @return what the merge returns
         * @throws IOException when the merge fails
         */
        T run() throws IOException;
    }
}
