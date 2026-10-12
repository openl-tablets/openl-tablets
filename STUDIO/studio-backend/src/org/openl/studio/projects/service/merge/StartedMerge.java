package org.openl.studio.projects.service.merge;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.Accessors;

import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.studio.projects.model.ProjectIdModel;
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
    private final ProjectIdModel projectId;

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
    boolean isSameAs(ProjectIdModel otherProjectId, String branch, MergeOpMode otherMode) {
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
}
