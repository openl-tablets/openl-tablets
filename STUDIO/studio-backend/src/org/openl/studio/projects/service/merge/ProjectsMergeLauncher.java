package org.openl.studio.projects.service.merge;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.SocketTimeoutException;
import java.nio.channels.ClosedByInterruptException;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.function.Supplier;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import org.openl.rules.common.ProjectException;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.repository.api.MergePublishing;
import org.openl.rules.workspace.ProjectId;
import org.openl.studio.common.exception.ConflictException;
import org.openl.studio.common.exception.NotFoundException;
import org.openl.studio.projects.messaging.ProjectSocketNotificationService;
import org.openl.studio.projects.model.merge.MergeOpMode;
import org.openl.studio.projects.model.merge.MergeResult;
import org.openl.studio.projects.service.ExecutionLifecycle;
import org.openl.studio.projects.service.ExecutionStatus;
import org.openl.studio.projects.service.WorkspaceProjectService;
import org.openl.util.RuntimeExceptionWrapper;

/**
 * Runs branch merges apart from the requests that start them.
 *
 * <p>A merge of a large project takes minutes, longer than a proxy waits for an answer. The request starting it
 * returns at once, the merge runs on its own pool, and its progress is reported on the WebSocket topic
 * {@code /user/topic/projects/{projectId}/merge/status}: {@code PENDING}, {@code STARTED}, then {@code COMPLETED},
 * {@code ERROR} with the reason, or {@code INTERRUPTED} for a merge cancelled on request.
 *
 * <p>A cancel interrupts the thread of the merge. The repository stops the merge before its next step and puts the
 * branches back as the merge found them, so a long merge stops within seconds and without an error.
 *
 * <p>The merge itself only works in the repository, and the project is read-only for the client meanwhile. The
 * editor state of the client — the opened project, its compilation, the conflict kept for resolution — belongs to
 * the client's requests, so the first request that finds the merge ended brings the editor up to date: it opens the
 * merged project again, or keeps the conflict.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProjectsMergeLauncher {

    /** What a merge ended without a result by a failure no handler takes, out of memory for one, reports. */
    static final String STOPPED = "The merge stopped before it finished.";

    private final ProjectsMergeService mergeService;
    private final WorkspaceProjectService projectService;
    private final ProjectsMergeRegistry registry;
    private final ProjectsMergeConflictsSessionHolder conflictsSessionHolder;
    private final ProjectSocketNotificationService notifications;
    @Qualifier("mergeExecutor")
    private final Executor mergeExecutor;

    /**
     * Starts merging the branches of a project, once the merge is validated.
     *
     * <p>The same merge asked for again while it runs — a client that sends the request again — is the merge already
     * running: nothing is validated or started again, and its result is read as usual. Another merge is refused until
     * the running one ends, because it would change the project the running one is merging.
     *
     * <p>A merge that has ended reaches the editor before another one starts, so its conflicts refuse the next merge
     * until they are resolved.
     *
     * @param project     the project to merge
     * @param otherBranch the other branch name (interpretation depends on mode)
     * @param mode        merge operation mode
     * @param force       confirms the bypass of the target branch protection
     * @throws ConflictException when another merge of this client is still running, the project has unresolved
     *                           conflicts, or the merge is refused
     * @throws IOException       when the repository cannot tell whether the merge may be performed
     */
    public void start(RulesProject project, String otherBranch, MergeOpMode mode, boolean force) throws IOException {
        var projectId = project.getProjectId();
        StartedMerge merge;
        // Two requests of the client never start merges at once: the second finds the merge the first has taken.
        var lock = registry.startLock();
        lock.lock();
        try {
            // Asked before the merge is validated: a running merge may have committed already, so there is nothing
            // left to merge.
            if (registry.isRunning(projectId, otherBranch, mode)) {
                return;
            }
            // No merge of this client runs now, so the one before has ended for good: its end reaches the editor
            // first.
            settleFinished();
            conflictsSessionHolder.requireResolved(projectId);
            mergeService.validateMergeAllowed(project, otherBranch, mode, force);
            var workspace = projectService.getUserWorkspace();
            merge = StartedMerge.builder()
                    .projectId(projectId)
                    .currentBranch(Objects.requireNonNull(project.getBranch()))
                    .otherBranch(otherBranch)
                    .mode(mode)
                    .userName(workspace.getUser().getUserName())
                    .editor(new StartedMerge.Editor(project,
                            workspace,
                            projectService.getWebStudio(),
                            project.getDesignRepository().getId(),
                            project.getRealPath()))
                    .build();
            registry.set(merge);
        } finally {
            lock.unlock();
        }
        report(merge, ExecutionStatus.PENDING);
        try {
            mergeExecutor.execute(() -> run(merge));
        } catch (RuntimeException e) {
            // Nothing was started: the merge ends here, and its result reports why.
            merge.task().completeExceptionally(e);
            throw e;
        }
    }

    /**
     * The result of the merge of the project this client started last.
     *
     * <p>Reading the result of an ended merge brings the editor up to date first, as every request of the client
     * does once the merge has ended.
     *
     * @param project the merged project
     * @return the result, or nothing while the merge is running
     * @throws NotFoundException when this client has started no merge of the project
     */
    public Optional<MergeResult> result(RulesProject project) {
        var merge = find(project).orElseThrow(() -> new NotFoundException("project.merge.not.found.message"));
        var task = merge.task();
        if (merge.isRunning()) {
            return Optional.empty();
        }
        settleOnce(merge);
        if (task.state() == Future.State.FAILED) {
            // The caller asked for the merge, not for the plumbing that ran it: report what it failed with.
            throw RuntimeExceptionWrapper.wrap(task.exceptionNow());
        }
        return Optional.of(task.resultNow());
    }

    /**
     * Whether a merge of the project this client started is running. The project is read-only for the client
     * meanwhile.
     *
     * @param project the project, resolved only while a merge of this client runs, which is seldom
     * @return {@code true} while the merge runs or waits for a thread
     */
    public boolean isRunning(Supplier<RulesProject> project) {
        return registry.current()
                .filter(StartedMerge::isRunning)
                .filter(merge -> merge.projectId().equals(project.get().getProjectId()))
                .isPresent();
    }

    /**
     * Stops the merge of the project this client started, when it is still running.
     *
     * <p>A merge waiting for a thread ends at once. A running merge is interrupted and stops before the next step it
     * can be stopped at — the repository lock it waits for, the next workbook it compares, at the latest before it
     * publishes the merge — leaving the branches as it found them. A merge that has started to publish finishes.
     * Either way its end is reported as usual: a stopped merge ends with {@link MergeResult#cancelled()}.
     *
     * @param project the project being merged
     * @return {@code true} when a merge of the project was running; {@code false} when there was none to stop
     */
    public boolean cancel(RulesProject project) {
        var merge = find(project).filter(StartedMerge::isRunning).orElse(null);
        if (merge == null) {
            return false;
        }
        if (merge.execution().cancel()) {
            endCancelled(merge);
        }
        return true;
    }

    /**
     * Brings the editor up to date after the merge of this client, once the merge has ended. A merge still running,
     * or one whose end has been taken care of, is left alone.
     *
     * <p>Whatever request finds the merge ended does it, so a failure is logged rather than raised: it is no failure
     * of that request, and the result of the merge stays what it is.
     */
    public void settleFinished() {
        registry.current().filter(merge -> !merge.isRunning()).ifPresent(this::settleOnce);
    }

    /**
     * Lets go of the ended merge of the project once the conflicts it left are resolved or cancelled. Its result would
     * report conflicts nobody has any more, and it holds what comparing the conflicted workbooks found. A merge that
     * runs is kept.
     *
     * @param projectId the merged project
     */
    public void forget(ProjectId projectId) {
        registry.forget(projectId);
    }

    private Optional<StartedMerge> find(RulesProject project) {
        return registry.find(project.getProjectId());
    }

    /**
     * Brings the editor up to date after the merge, once. Requests that find the merge ended meanwhile wait until it
     * is done, so none of them goes on with a project that is being opened again.
     */
    private void settleOnce(StartedMerge merge) {
        merge.settleOnce(() -> {
            try {
                settle(merge);
            } catch (ProjectException | RuntimeException e) {
                log.error("Failed to bring the editor up to date after the merge of the project '{}'.",
                        merge.editor().realPath(),
                        e);
            }
        });
    }

    private void run(StartedMerge merge) {
        if (!merge.execution().enter()) {
            // Cancelled while it waited for a thread: the cancel has ended it.
            return;
        }
        try {
            report(merge, ExecutionStatus.STARTED);
            var editor = merge.editor();
            // Completed before it is announced: a client that reads the result on the announcement finds it. The
            // repository says when the merge starts to publish, from when a cancel no longer interrupts it.
            merge.task().complete(MergePublishing.listen(merge.execution()::publishing,
                    () -> mergeService.merge(editor.project(),
                            merge.currentBranch(),
                            merge.otherBranch(),
                            merge.mode(),
                            editor.workspace(),
                            editor.studio())));
            report(merge, ExecutionStatus.COMPLETED);
        } catch (IOException | RuntimeException e) {
            if (Thread.currentThread().isInterrupted() && causedByInterrupt(e)) {
                // Stopped on request: the repository has put the branches back as the merge found them.
                endCancelled(merge);
            } else {
                fail(merge, e);
            }
        } finally {
            merge.execution().leave();
            // A failure no handler takes - out of memory - still ends the merge: a merge that never ends would
            // refuse every later merge of the client.
            if (merge.task().completeExceptionally(new IllegalStateException(STOPPED))) {
                reportError(merge, STOPPED);
            }
        }
    }

    /**
     * Whether a failure is the interrupt cutting the merge off, rather than a failure the interrupt merely came
     * along with. A timeout of the network is an interrupted read too, but no cancel.
     */
    private static boolean causedByInterrupt(Throwable failure) {
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            if (cause instanceof InterruptedIOException && !(cause instanceof SocketTimeoutException)
                    || cause instanceof ClosedByInterruptException
                    || cause instanceof InterruptedException) {
                return true;
            }
        }
        return false;
    }

    private void fail(StartedMerge merge, Exception e) {
        log.error("Failed to merge the branch '{}' of the project '{}' in the repository '{}'.",
                merge.otherBranch(),
                merge.editor().realPath(),
                merge.editor().repositoryId(),
                e);
        merge.task().completeExceptionally(e);
        reportError(merge, ExecutionLifecycle.reasonOf(e));
    }

    /** Ends a merge stopped on request, unless it has ended already, and reports it as interrupted. */
    private void endCancelled(StartedMerge merge) {
        if (merge.task().complete(MergeResult.cancelled())) {
            report(merge, ExecutionStatus.INTERRUPTED);
        }
    }

    private void report(StartedMerge merge, ExecutionStatus status) {
        reportQuietly(merge, () -> notifications.notifyMergeStatus(merge.userName(), merge.projectId(), status));
    }

    private void reportError(StartedMerge merge, String reason) {
        reportQuietly(merge, () -> notifications.notifyMergeError(merge.userName(), merge.projectId(), reason));
    }

    /**
     * Tells the client how the merge goes. A report that cannot be sent changes nothing in the merge: its result is
     * read from the result endpoint all the same.
     */
    private static void reportQuietly(StartedMerge merge, Runnable report) {
        try {
            report.run();
        } catch (RuntimeException e) {
            log.warn("Failed to report the progress of the merge of the project '{}'.",
                    merge.editor().realPath(),
                    e);
        }
    }

    /**
     * Brings the editor up to date after an ended merge: a merged project is opened again, and a conflict is kept for
     * resolution. A failed or a cancelled merge changed nothing, so it leaves the editor as it is.
     */
    private void settle(StartedMerge merge) throws ProjectException {
        if (merge.task().state() != Future.State.SUCCESS) {
            return;
        }
        var result = merge.task().resultNow();
        switch (result.status()) {
            case SUCCESS -> reopen(merge);
            case CONFLICTS -> conflictsSessionHolder.store(merge.projectId(), result.conflictInfo());
            case CANCELLED -> log.debug("The merge of the project '{}' is cancelled.",
                    merge.editor().realPath());
        }
    }

    /**
     * Opens a merged project again, so the editor shows what the merge committed, and resets the editor session.
     *
     * <p>A project with changes of its own, or switched to another branch meanwhile — by another client of the user,
     * as the workspace is shared — is left as it is: closing it would drop those changes. Saving them merges them with
     * what the merge committed.
     */
    private void reopen(StartedMerge merge) throws ProjectException {
        var editor = merge.editor();
        var project = editor.workspace()
                .getProjectByPath(editor.repositoryId(), editor.realPath())
                .orElse(editor.project());
        if (project.isOpened() && (project.isModified() || !merge.currentBranch().equals(project.getBranch()))) {
            log.info("The project '{}' has changed while it was merged, so it is not opened again.",
                    project.getName());
            return;
        }
        var studio = editor.studio();
        var model = project.isOpened() ? studio.getModel() : null;
        // The compilation is paused while the project is opened again, so it does not read a project being closed —
        // see EPBDS-12353. Only a failure lets it go again: the reset drops the dependency manager.
        var compilation = model == null ? null : model.getWebStudioWorkspaceDependencyManager();
        if (compilation != null) {
            compilation.pause();
        }
        var shouldResumeCompilation = true;
        try {
            var nameBeforeMerge = project.getName();
            var nameAfterMerge = model == null ? nameBeforeMerge : reopenMergedProject(merge, project);
            studio.reset();
            if (model != null) {
                model.clearModuleInfo();
            }
            shouldResumeCompilation = false;
            if (!nameAfterMerge.equals(nameBeforeMerge)) {
                studio.init(editor.repositoryId(), merge.currentBranch(), nameAfterMerge, null);
            }
        } finally {
            if (shouldResumeCompilation && compilation != null) {
                compilation.resume();
            }
        }
    }

    /**
     * Opens the merged project again, unless the merge deleted it.
     *
     * @return the name of the project after the merge, which can rename it
     */
    private static String reopenMergedProject(StartedMerge merge, RulesProject project) throws ProjectException {
        var nameBeforeMerge = project.getName();
        if (project.isDeleted()) {
            project.close();
            return nameBeforeMerge;
        }
        // Project can be renamed after merge, so we close it before opening to ensure that
        // project folder name in editor is up to date.
        project.close();
        var editor = merge.editor();
        var workspace = editor.workspace();
        workspace.refresh();

        var refreshedProject = workspace.getProjectByPath(editor.repositoryId(), editor.realPath());
        if (refreshedProject.isEmpty()) {
            return nameBeforeMerge;
        }
        var mergedProject = refreshedProject.get();
        workspace.setProjectBranch(mergedProject, merge.currentBranch());
        mergedProject.open();
        return mergedProject.getName();
    }
}
