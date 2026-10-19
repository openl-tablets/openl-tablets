package org.openl.studio.projects.service.merge;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.jspecify.annotations.Nullable;

import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.workspace.ProjectId;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.studio.projects.model.merge.MergeOpMode;
import org.openl.studio.projects.model.merge.MergeResult;

/**
 * A branch merge a client has started, and the editor state it has to bring up to date once it ends.
 *
 * <p>The merge works on the branches as they were validated: the branch the project was on when the merge started is
 * the one merged, whatever the project is switched to meanwhile. The editor is brought up to date once, by the first
 * request of the client that finds the merge ended ({@link #settleOnce(Runnable)}).
 */
@Getter
@Accessors(fluent = true)
@Builder
final class StartedMerge {

    /** The project being merged. */
    private final ProjectId projectId;

    /** The branch the project was on when the merge started. */
    private final String currentBranch;

    /** The other branch of the merge. */
    private final String otherBranch;

    /** Whether the other branch is merged into the current one, or the current one into it. */
    private final MergeOpMode mode;

    /** The user the progress of the merge is reported to. */
    private final String userName;

    /** The editor state as the merge found it. */
    private final Editor editor;

    /** The merge, which ends with its result. */
    private final CompletableFuture<MergeResult> task = new CompletableFuture<>();

    /** The thread running the merge, which a cancel stops. */
    private final Execution execution = new Execution();

    @Getter(AccessLevel.NONE)
    private final AtomicBoolean settled = new AtomicBoolean();

    /** Whether the merge runs, or waits for a thread. */
    boolean isRunning() {
        return !task.isDone();
    }

    /**
     * Brings the editor up to date after the merge, once: the first caller runs the update, and every other caller
     * waits until it is done.
     *
     * @param update what brings the editor up to date
     */
    synchronized void settleOnce(Runnable update) {
        if (settled.compareAndSet(false, true)) {
            update.run();
        }
    }

    /** Whether this is the merge of the same branches of the project, in the same direction. */
    boolean isSameAs(ProjectId otherProjectId, String branch, MergeOpMode otherMode) {
        return projectId.equals(otherProjectId) && otherBranch.equals(branch) && mode == otherMode;
    }

    /**
     * The editor state of the project a merge changes.
     *
     * <p>The workspace builds its projects anew whenever it is refreshed, so the project is looked up again by where it
     * is kept rather than read from the instance the merge started with.
     *
     * @param project      the project as the merge found it
     * @param workspace    the workspace of the user who merges
     * @param studio       the editor session of the client
     * @param repositoryId the repository the project is kept in
     * @param realPath     where the project is kept in the repository, which a rename leaves as it is
     */
    record Editor(RulesProject project,
                  UserWorkspace workspace,
                  WebStudio studio,
                  String repositoryId,
                  String realPath) {
    }

    /**
     * The thread a merge runs on, and whether the merge is asked to stop.
     *
     * <p>A merge is stopped by interrupting its thread, which belongs to a pool: the interrupt reaches the thread only
     * while it runs this merge, never the work the pool gives it next, and only until the merge starts to publish its
     * result, which it then finishes.
     */
    static final class Execution {

        private @Nullable Thread runner;
        private boolean cancelled;
        private boolean publishing;

        /**
         * Takes the current thread as the one running the merge.
         *
         * @return {@code false} when the merge was cancelled before it started: it must not run
         */
        synchronized boolean enter() {
            if (cancelled) {
                return false;
            }
            runner = Thread.currentThread();
            return true;
        }

        /**
         * Lets the current thread go: a cancel asked for afterwards does not reach it.
         *
         * <p>An interrupt that came too late to stop the merge stays set, as an interrupt is never swallowed: the pool
         * clears it before it gives the thread other work, unless the pool is stopping.
         */
        synchronized void leave() {
            runner = null;
        }

        /**
         * Marks the merge as publishing its result: it finishes from now on, so a cancel no longer interrupts it.
         */
        synchronized void publishing() {
            publishing = true;
        }

        /**
         * Asks the merge to stop: a running merge is interrupted, unless it publishes its result already, and a merge
         * waiting for a thread is not run.
         *
         * @return {@code true} when the merge has not started: nothing runs it, so the caller ends it
         */
        synchronized boolean cancel() {
            cancelled = true;
            if (runner != null) {
                if (!publishing) {
                    runner.interrupt();
                }
                return false;
            }
            return true;
        }
    }
}
