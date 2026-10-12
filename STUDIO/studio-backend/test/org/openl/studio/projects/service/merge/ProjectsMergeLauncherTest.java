package org.openl.studio.projects.service.merge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.nio.channels.ClosedByInterruptException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.OngoingStubbing;

import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.repository.api.BranchRepository;
import org.openl.rules.repository.api.MergePublishing;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.webstudio.dependencies.WebStudioWorkspaceRelatedDependencyManager;
import org.openl.rules.workspace.WorkspaceUser;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.studio.common.exception.ConflictException;
import org.openl.studio.common.exception.NotFoundException;
import org.openl.studio.projects.messaging.ProjectSocketNotificationService;
import org.openl.studio.projects.model.ProjectIdModel;
import org.openl.studio.projects.model.merge.MergeConflictInfo;
import org.openl.studio.projects.model.merge.MergeOpMode;
import org.openl.studio.projects.model.merge.MergeResult;
import org.openl.studio.projects.model.merge.MergeResultStatus;
import org.openl.studio.projects.service.ExecutionStatus;
import org.openl.studio.projects.service.ProjectIdentifierMapper;
import org.openl.studio.projects.service.WorkspaceProjectService;

/**
 * A merge runs apart from the request that starts it, and the editor is brought up to date by the first request
 * of the client that finds it ended — see EPBDS-16469.
 */
class ProjectsMergeLauncherTest {

    private static final String CURRENT = "dev";
    private static final String OTHER = "master";
    private static final String USER = "admin";
    private static final String REAL_PATH = "DESIGN/rules/RateCalculator";

    private final List<Runnable> queued = new ArrayList<>();

    private ProjectsMergeLauncher launcher;
    private RulesProject project;
    private ProjectIdModel projectId;
    private ProjectsMergeService mergeService;
    private WorkspaceProjectService projectService;
    private UserWorkspace workspace;
    private WebStudio studio;
    private ProjectModel model;
    private WebStudioWorkspaceRelatedDependencyManager compilation;
    private ProjectsMergeRegistry registry;
    private ProjectsMergeConflictsSessionHolder conflictsSessionHolder;
    private ProjectSocketNotificationService notifications;
    private ProjectIdentifierMapper projectIdentifierMapper;

    @BeforeEach
    void init() {
        var repository = mock(BranchRepository.class);
        lenient().when(repository.getId()).thenReturn("design");

        project = mock(RulesProject.class);
        lenient().when(project.getName()).thenReturn("RateCalculator");
        lenient().when(project.getBranch()).thenReturn(CURRENT);
        lenient().when(project.getRealPath()).thenReturn(REAL_PATH);
        lenient().when(project.getDesignRepository()).thenReturn(repository);

        compilation = mock(WebStudioWorkspaceRelatedDependencyManager.class);
        model = mock(ProjectModel.class);
        lenient().when(model.getWebStudioWorkspaceDependencyManager()).thenReturn(compilation);

        var user = mock(WorkspaceUser.class);
        lenient().when(user.getUserName()).thenReturn(USER);
        workspace = mock(UserWorkspace.class);
        lenient().when(workspace.getUser()).thenReturn(user);
        lenient().when(workspace.getProjectByPath("design", REAL_PATH)).thenReturn(Optional.of(project));
        studio = mock(WebStudio.class);
        lenient().when(studio.getModel()).thenReturn(model);
        projectService = mock(WorkspaceProjectService.class);
        lenient().when(projectService.getUserWorkspace()).thenReturn(workspace);
        lenient().when(projectService.getWebStudio()).thenReturn(studio);

        projectId = ProjectIdModel.builder().repository("design").projectName("RateCalculator").build();
        projectIdentifierMapper = mock(ProjectIdentifierMapper.class);
        lenient().when(projectIdentifierMapper.map(project)).thenReturn(projectId);

        mergeService = mock(ProjectsMergeService.class);
        registry = new ProjectsMergeRegistry();
        conflictsSessionHolder = new ProjectsMergeConflictsSessionHolder();
        notifications = mock(ProjectSocketNotificationService.class);
        // Its merges wait in a queue until the test runs them.
        launcher = launcher(queued::add);
    }

    /** A cancel interrupts the thread running the merge, here the thread of the test: the next test starts clear. */
    @AfterEach
    void clearInterrupt() {
        Thread.interrupted();
    }

    private ProjectsMergeLauncher launcher(Executor executor) {
        return new ProjectsMergeLauncher(mergeService,
                projectService,
                registry,
                conflictsSessionHolder,
                projectIdentifierMapper,
                notifications,
                executor);
    }

    private void runQueued() {
        queued.forEach(Runnable::run);
        queued.clear();
    }

    private OngoingStubbing<MergeResult> whenMerging(MergeOpMode mode) throws IOException {
        return when(mergeService.merge(project, CURRENT, OTHER, mode, workspace));
    }

    private RulesProject otherProject() {
        var other = mock(RulesProject.class);
        when(projectIdentifierMapper.map(other))
                .thenReturn(ProjectIdModel.builder().repository("design").projectName("Other").build());
        return other;
    }

    /** The workspace builds the project anew: the merged project after it is refreshed. */
    private RulesProject givenMergedProject(String name) {
        var mergedProject = mock(RulesProject.class);
        lenient().when(mergedProject.getName()).thenReturn(name);
        when(workspace.getProjectByPath("design", REAL_PATH))
                .thenReturn(Optional.of(project), Optional.of(mergedProject));
        return mergedProject;
    }

    @Test
    void answersBeforeTheMergeRunsAndReportsItsProgress() throws Exception {
        whenMerging(MergeOpMode.RECEIVE).thenReturn(MergeResult.builder().build());

        launcher.start(project, OTHER, MergeOpMode.RECEIVE, true);

        verify(mergeService).validateMergeAllowed(project, OTHER, MergeOpMode.RECEIVE, true);
        // The request has answered, and the merge has not run yet: its result is not ready, which is no error.
        verify(mergeService, never()).merge(any(), any(), any(), any(), any());
        assertEquals(Optional.empty(), launcher.result(project));
        assertTrue(launcher.isRunning(() -> project));

        runQueued();

        var order = inOrder(notifications);
        order.verify(notifications).notifyMergeStatus(USER, projectId, ExecutionStatus.PENDING);
        order.verify(notifications).notifyMergeStatus(USER, projectId, ExecutionStatus.STARTED);
        order.verify(notifications).notifyMergeStatus(USER, projectId, ExecutionStatus.COMPLETED);
        assertEquals(MergeResultStatus.SUCCESS, launcher.result(project).orElseThrow().status());
        assertFalse(launcher.isRunning(() -> project));
    }

    /** The request answers at once, even for a project the editor is still compiling. */
    @Test
    void startsWithoutWaitingForTheCompilation() throws Exception {
        when(project.isOpened()).thenReturn(true);

        launcher.start(project, OTHER, MergeOpMode.SEND, false);

        verify(projectService, never()).openProject(any());
        verify(projectService, never()).openProject(any(), any());
        assertEquals(1, queued.size());
    }

    @Test
    void startsNothingTheValidationRefuses() throws Exception {
        doThrow(new ConflictException("project.branch.merge.not.mergeable.message"))
                .when(mergeService).validateMergeAllowed(project, OTHER, MergeOpMode.SEND, false);

        assertThrows(ConflictException.class, () -> launcher.start(project, OTHER, MergeOpMode.SEND, false));

        assertTrue(queued.isEmpty());
        assertThrows(NotFoundException.class, () -> launcher.result(project));
    }

    @Test
    void refusesAMergeWhileTheProjectHasUnresolvedConflicts() throws Exception {
        conflictsSessionHolder.store(projectId, mock(MergeConflictInfo.class));

        assertThrows(ConflictException.class, () -> launcher.start(project, OTHER, MergeOpMode.SEND, false));

        verify(mergeService, never()).validateMergeAllowed(any(), any(), any(), anyBoolean());
        assertTrue(queued.isEmpty());
    }

    /** The conflicts of a merge that has ended refuse the next merge, though no request has read them yet. */
    @Test
    void bringsAnEndedMergeToTheEditorBeforeTheNextStarts() throws Exception {
        whenMerging(MergeOpMode.SEND)
                .thenReturn(MergeResult.builder().conflictInfo(mock(MergeConflictInfo.class)).build());
        launcher.start(project, OTHER, MergeOpMode.SEND, false);
        runQueued();

        assertThrows(ConflictException.class, () -> launcher.start(project, "release", MergeOpMode.SEND, false));

        assertTrue(conflictsSessionHolder.hasConflictInfo(projectId));
        verify(mergeService, times(1)).validateMergeAllowed(any(), any(), any(), anyBoolean());
    }

    @Test
    void mergesAClosedProjectWithoutOpeningIt() throws Exception {
        whenMerging(MergeOpMode.SEND).thenReturn(MergeResult.builder().build());

        launcher.start(project, OTHER, MergeOpMode.SEND, false);
        runQueued();

        assertEquals(MergeResultStatus.SUCCESS, launcher.result(project).orElseThrow().status());
        // Opening it would compile a project nobody is looking at — and used to fail with "not opened".
        verify(project, never()).open();
        verify(project, never()).close();
        verify(compilation, never()).pause();
        verify(studio).reset();
    }

    /**
     * The merge works in the repository only, so the editor keeps compiling the project while it runs. A client
     * that reloads the page in the middle of a merge, and so never asks for its result, keeps a working editor.
     */
    @Test
    void theEditorKeepsCompilingWhileTheMergeRuns() throws Exception {
        when(project.isOpened()).thenReturn(true);
        whenMerging(MergeOpMode.SEND).thenReturn(MergeResult.builder().build());

        launcher.start(project, OTHER, MergeOpMode.SEND, false);
        runQueued();

        verify(compilation, never()).pause();
        verify(project, never()).close();
    }

    @Test
    void reopensAnOpenedProjectSoTheEditorShowsTheMergedContent() throws Exception {
        when(project.isOpened()).thenReturn(true);
        var mergedProject = givenMergedProject("RateCalculator");
        whenMerging(MergeOpMode.SEND).thenReturn(MergeResult.builder().build());
        launcher.start(project, OTHER, MergeOpMode.SEND, false);
        runQueued();

        assertEquals(MergeResultStatus.SUCCESS, launcher.result(project).orElseThrow().status());

        // The compilation stops reading the project before the project is closed — see EPBDS-12353.
        var order = inOrder(compilation, project, mergedProject, studio);
        order.verify(compilation).pause();
        order.verify(project).close();
        order.verify(mergedProject).open();
        order.verify(studio).reset();
        verify(workspace).setProjectBranch(mergedProject, CURRENT);
        verify(model).clearModuleInfo();
        // The reset drops the dependency manager it paused.
        verify(compilation, never()).resume();
        verify(studio, never()).init(anyString(), anyString(), anyString(), any());
    }

    /** The workspace builds its projects anew on a refresh, so the instance the merge started with may be old. */
    @Test
    void reopensTheProjectAsTheWorkspaceHoldsItNow() throws Exception {
        var current = mock(RulesProject.class);
        when(current.isOpened()).thenReturn(true);
        when(current.getBranch()).thenReturn(CURRENT);
        when(current.getName()).thenReturn("RateCalculator");
        var mergedProject = mock(RulesProject.class);
        when(mergedProject.getName()).thenReturn("RateCalculator");
        when(workspace.getProjectByPath("design", REAL_PATH))
                .thenReturn(Optional.of(current), Optional.of(mergedProject));
        whenMerging(MergeOpMode.SEND).thenReturn(MergeResult.builder().build());
        launcher.start(project, OTHER, MergeOpMode.SEND, false);
        runQueued();

        launcher.result(project);

        verify(current).close();
        verify(project, never()).close();
        verify(mergedProject).open();
    }

    /** Closing a project drops its changes, so a project changed while it was merged is left as it is. */
    @Test
    void keepsTheChangesMadeWhileTheMergeRan() throws Exception {
        when(project.isOpened()).thenReturn(true);
        when(project.isModified()).thenReturn(true);
        whenMerging(MergeOpMode.RECEIVE).thenReturn(MergeResult.builder().build());
        launcher.start(project, OTHER, MergeOpMode.RECEIVE, false);
        runQueued();

        assertEquals(MergeResultStatus.SUCCESS, launcher.result(project).orElseThrow().status());

        verify(project, never()).close();
        verify(studio, never()).reset();
        verify(compilation, never()).pause();
    }

    /** Another client of the user, which shares the workspace, may switch the project meanwhile. */
    @Test
    void keepsAProjectSwitchedToAnotherBranchMeanwhile() throws Exception {
        when(project.isOpened()).thenReturn(true);
        whenMerging(MergeOpMode.RECEIVE).thenReturn(MergeResult.builder().build());
        launcher.start(project, OTHER, MergeOpMode.RECEIVE, false);
        when(project.getBranch()).thenReturn("release");
        runQueued();

        launcher.result(project);

        verify(project, never()).close();
        verify(workspace, never()).setProjectBranch(any(), anyString());
    }

    /** The branches merged are the ones validated, whatever the project is switched to while the merge waits. */
    @Test
    void mergesTheBranchTheMergeWasValidatedWith() throws Exception {
        whenMerging(MergeOpMode.RECEIVE).thenReturn(MergeResult.builder().build());
        launcher.start(project, OTHER, MergeOpMode.RECEIVE, false);
        when(project.getBranch()).thenReturn("release");

        runQueued();

        verify(mergeService).merge(project, CURRENT, OTHER, MergeOpMode.RECEIVE, workspace);
    }

    /** The result of the merge stays what it is: the failure belongs to no request that happened to settle it. */
    @Test
    void logsAProjectThatCannotBeOpenedAgainAndKeepsTheResult() throws Exception {
        when(project.isOpened()).thenReturn(true);
        var mergedProject = givenMergedProject("RateCalculator");
        doThrow(new IllegalStateException("The project folder is locked")).when(mergedProject).open();
        whenMerging(MergeOpMode.SEND).thenReturn(MergeResult.builder().build());
        launcher.start(project, OTHER, MergeOpMode.SEND, false);
        runQueued();

        assertEquals(MergeResultStatus.SUCCESS, launcher.result(project).orElseThrow().status());

        verify(compilation).resume();
        verify(studio, never()).reset();
        // Once is enough: the project is closed by now, and closing it again would find nothing to close.
        launcher.settleFinished();
        verify(project, times(1)).close();
    }

    @Test
    void opensARenamedProjectByItsNewName() throws Exception {
        when(project.isOpened()).thenReturn(true);
        givenMergedProject("RateEngine");
        whenMerging(MergeOpMode.SEND).thenReturn(MergeResult.builder().build());

        launcher.start(project, OTHER, MergeOpMode.SEND, false);
        runQueued();
        launcher.result(project);

        verify(studio).init("design", CURRENT, "RateEngine", null);
    }

    @Test
    void keepsAProjectTheMergeDeletedClosed() throws Exception {
        when(project.isOpened()).thenReturn(true);
        when(project.isDeleted()).thenReturn(true);
        whenMerging(MergeOpMode.SEND).thenReturn(MergeResult.builder().build());

        launcher.start(project, OTHER, MergeOpMode.SEND, false);
        runQueued();
        launcher.result(project);

        verify(project).close();
        verify(workspace, never()).refresh();
        verify(studio).reset();
    }

    @Test
    void keepsTheConflictForResolutionAndLeavesTheEditorAsItIs() throws Exception {
        when(project.isOpened()).thenReturn(true);
        var conflictInfo = mock(MergeConflictInfo.class);
        whenMerging(MergeOpMode.RECEIVE).thenReturn(MergeResult.builder().conflictInfo(conflictInfo).build());

        launcher.start(project, OTHER, MergeOpMode.RECEIVE, false);
        runQueued();
        // A request of the client that asks for the conflict, rather than the result, finds it all the same.
        launcher.settleFinished();

        assertSame(conflictInfo, conflictsSessionHolder.getConflictInfo(projectId));
        verify(compilation, never()).pause();
        verify(studio, never()).reset();
        assertEquals(MergeResultStatus.CONFLICTS, launcher.result(project).orElseThrow().status());
    }

    @Test
    void bringsTheEditorUpToDateOnce() throws Exception {
        whenMerging(MergeOpMode.SEND).thenReturn(MergeResult.builder().build());

        launcher.start(project, OTHER, MergeOpMode.SEND, false);
        runQueued();
        launcher.result(project);
        launcher.result(project);
        launcher.settleFinished();

        verify(studio, times(1)).reset();
    }

    @Test
    void reportsWhatAFailedMergeFailedWith() throws Exception {
        when(project.isOpened()).thenReturn(true);
        whenMerging(MergeOpMode.SEND)
                .thenThrow(new IOException("Repository is not available"));

        launcher.start(project, OTHER, MergeOpMode.SEND, false);
        runQueued();

        verify(notifications).notifyMergeError(USER, projectId, "Repository is not available");
        var failure = assertThrows(RuntimeException.class, () -> launcher.result(project));
        assertTrue(failure.getCause() instanceof IOException);
        // A failed merge changed nothing, so the editor is left as it is.
        verify(compilation, never()).pause();
        verify(studio, never()).reset();
    }

    @Test
    void endsAMergeThatFailedPastEveryHandler() throws Exception {
        whenMerging(MergeOpMode.SEND)
                .thenThrow(new StackOverflowError());

        launcher.start(project, OTHER, MergeOpMode.SEND, false);
        assertThrows(StackOverflowError.class, this::runQueued);

        verify(notifications).notifyMergeError(USER, projectId, ProjectsMergeLauncher.STOPPED);
        // Reported as the failure it is, not as a conflict of the merge.
        var failure = assertThrows(IllegalStateException.class, () -> launcher.result(project));
        assertEquals(ProjectsMergeLauncher.STOPPED, failure.getMessage());
        // A merge that never ended would refuse every later merge of the client.
        launcher.start(project, OTHER, MergeOpMode.SEND, false);
    }

    @Test
    void refusesAnotherMergeWhileTheFirstRuns() throws Exception {
        launcher.start(project, OTHER, MergeOpMode.SEND, false);

        assertThrows(ConflictException.class, () -> launcher.start(project, OTHER, MergeOpMode.RECEIVE, false));
        assertThrows(ConflictException.class, () -> launcher.start(project, "release", MergeOpMode.SEND, false));

        // Refused before the merge is validated.
        verify(mergeService, times(1)).validateMergeAllowed(any(), any(), any(), anyBoolean());
    }

    /** A client sends the request again: the merge is the one running. */
    @Test
    void takesTheSameMergeAskedForAgainForTheOneRunning() throws Exception {
        whenMerging(MergeOpMode.SEND).thenReturn(MergeResult.builder().build());
        launcher.start(project, OTHER, MergeOpMode.SEND, false);
        // The running merge may have committed already, so the merge would have nothing left to merge.
        doThrow(new ConflictException("project.branch.merge.not.mergeable.message"))
                .when(mergeService).validateMergeAllowed(project, OTHER, MergeOpMode.SEND, false);

        launcher.start(project, OTHER, MergeOpMode.SEND, false);
        runQueued();

        verify(mergeService, times(1)).merge(any(), any(), any(), any(), any());
        verify(notifications, times(1)).notifyMergeStatus(USER, projectId, ExecutionStatus.PENDING);
        assertEquals(MergeResultStatus.SUCCESS, launcher.result(project).orElseThrow().status());
    }

    /**
     * Two requests of the client never start merges at once: the second one finds the merge the first has taken, so
     * the same merge sent twice runs once.
     */
    @Test
    void startsTheMergesOfTheClientOneAtATime() throws Exception {
        doAnswer(invocation -> {
            // Another request of the client, here another thread, waits while this one validates the merge.
            assertFalse(CompletableFuture.supplyAsync(this::takesTheStartLock).join());
            return null;
        }).when(mergeService).validateMergeAllowed(project, OTHER, MergeOpMode.SEND, false);

        launcher.start(project, OTHER, MergeOpMode.SEND, false);

        assertTrue(CompletableFuture.supplyAsync(this::takesTheStartLock).join());
    }

    private boolean takesTheStartLock() {
        var lock = registry.startLock();
        var taken = lock.tryLock();
        if (taken) {
            lock.unlock();
        }
        return taken;
    }

    @Test
    void startsAnotherMergeOnceTheFirstHasEnded() throws Exception {
        whenMerging(MergeOpMode.SEND).thenReturn(MergeResult.builder().build());
        launcher.start(project, OTHER, MergeOpMode.SEND, false);
        runQueued();

        launcher.start(project, OTHER, MergeOpMode.SEND, false);

        // The editor is brought up to date after the first merge before the second one starts.
        verify(studio).reset();
        assertEquals(Optional.empty(), launcher.result(project));
    }

    @Test
    void reportsAMergeThatCannotBeStarted() throws Exception {
        var refusing = launcher(command -> {
            throw new RejectedExecutionException("The pool is full");
        });

        assertThrows(RejectedExecutionException.class,
                () -> refusing.start(project, OTHER, MergeOpMode.SEND, false));

        verify(mergeService, never()).merge(any(), any(), any(), any(), any());
        assertThrows(RejectedExecutionException.class, () -> refusing.result(project));
    }

    @Test
    void endsAMergeCancelledWhileItWaitsForAThread() throws Exception {
        launcher.start(project, OTHER, MergeOpMode.SEND, false);

        assertTrue(launcher.cancel(project));

        // Ended at once: a thread it waits for may be busy with another merge for minutes.
        assertEquals(MergeResultStatus.CANCELLED, launcher.result(project).orElseThrow().status());
        verify(notifications).notifyMergeStatus(USER, projectId, ExecutionStatus.INTERRUPTED);
        runQueued();
        verify(mergeService, never()).merge(any(), any(), any(), any(), any());
        verify(notifications, never()).notifyMergeStatus(USER, projectId, ExecutionStatus.STARTED);
    }

    /** A cancel interrupts the merge, which the repository stops before its next step — see GitRepository. */
    @Test
    void stopsARunningMergeOnCancel() throws Exception {
        when(project.isOpened()).thenReturn(true);
        whenMerging(MergeOpMode.SEND).thenAnswer(invocation -> {
            assertTrue(launcher.cancel(project));
            assertTrue(Thread.currentThread().isInterrupted());
            throw new InterruptedIOException("The merge is cancelled.");
        });
        launcher.start(project, OTHER, MergeOpMode.SEND, false);

        runQueued();

        // The interrupt is never swallowed, here the thread of the test.
        assertTrue(Thread.currentThread().isInterrupted());
        verify(notifications).notifyMergeStatus(USER, projectId, ExecutionStatus.INTERRUPTED);
        verify(notifications, never()).notifyMergeError(any(), any(), any());
        assertEquals(MergeResultStatus.CANCELLED, launcher.result(project).orElseThrow().status());
        // The repository is as the merge found it, so the editor is left as it is.
        verify(compilation, never()).pause();
        verify(studio, never()).reset();
        // A cancelled merge refuses no later one.
        launcher.start(project, "release", MergeOpMode.SEND, false);
    }

    @Test
    void reportsAMergeCutOffByTheCancelAsCancelled() throws Exception {
        whenMerging(MergeOpMode.SEND).thenAnswer(invocation -> {
            launcher.cancel(project);
            // A Git write the interrupt cut off fails with whatever the write failed with.
            throw new IOException("Cannot write the index", new ClosedByInterruptException());
        });
        launcher.start(project, OTHER, MergeOpMode.SEND, false);

        runQueued();

        assertEquals(MergeResultStatus.CANCELLED, launcher.result(project).orElseThrow().status());
        verify(notifications, never()).notifyMergeError(any(), any(), any());
    }

    /** A failure the interrupt did not cause is a failure, even when a cancel came along with it. */
    @Test
    void reportsAFailureThatCameAlongWithTheCancel() throws Exception {
        whenMerging(MergeOpMode.SEND).thenAnswer(invocation -> {
            launcher.cancel(project);
            throw new IOException("The push is rejected");
        });
        launcher.start(project, OTHER, MergeOpMode.SEND, false);

        runQueued();

        verify(notifications).notifyMergeError(USER, projectId, "The push is rejected");
        assertThrows(RuntimeException.class, () -> launcher.result(project));
    }

    @Test
    void finishesAMergeThatHasStartedToPublish() throws Exception {
        whenMerging(MergeOpMode.SEND).thenAnswer(invocation -> {
            // The repository announces the push: cut off half way, it may have reached the remote repository.
            MergePublishing.announce();
            assertTrue(launcher.cancel(project));
            // So the cancel no longer interrupts the merge.
            assertFalse(Thread.currentThread().isInterrupted());
            return MergeResult.builder().build();
        });
        launcher.start(project, OTHER, MergeOpMode.SEND, false);

        runQueued();

        verify(notifications).notifyMergeStatus(USER, projectId, ExecutionStatus.COMPLETED);
        assertEquals(MergeResultStatus.SUCCESS, launcher.result(project).orElseThrow().status());
        verify(studio).reset();
    }

    /** A status the socket cannot send changes nothing in the merge: its result is read all the same. */
    @Test
    void keepsAMergeWhoseEndCannotBeReported() throws Exception {
        whenMerging(MergeOpMode.SEND).thenReturn(MergeResult.builder().build());
        doThrow(new IllegalStateException("The broker is down"))
                .when(notifications).notifyMergeStatus(USER, projectId, ExecutionStatus.COMPLETED);
        launcher.start(project, OTHER, MergeOpMode.SEND, false);

        runQueued();

        assertEquals(MergeResultStatus.SUCCESS, launcher.result(project).orElseThrow().status());
        verify(notifications, never()).notifyMergeError(any(), any(), any());
    }

    /** Once its conflicts are resolved or cancelled, a merge has nothing left to report. */
    @Test
    void letsGoOfAMergeWhoseConflictsAreDone() throws Exception {
        whenMerging(MergeOpMode.SEND)
                .thenReturn(MergeResult.builder().conflictInfo(mock(MergeConflictInfo.class)).build());
        launcher.start(project, OTHER, MergeOpMode.SEND, false);
        runQueued();

        launcher.forget(projectId);

        assertThrows(NotFoundException.class, () -> launcher.result(project));
    }

    @Test
    void keepsARunningMergeWhenAskedToLetGoOfIt() throws Exception {
        launcher.start(project, OTHER, MergeOpMode.SEND, false);

        launcher.forget(projectId);

        assertEquals(Optional.empty(), launcher.result(project));
    }

    /** A request that finds the merge ended while another one opens the project again waits until it is open. */
    @Test
    void waitsForTheEditorAnotherRequestBringsUpToDate() throws Exception {
        when(project.isOpened()).thenReturn(true);
        var mergedProject = givenMergedProject("RateCalculator");
        var opening = new CountDownLatch(1);
        var opened = new CountDownLatch(1);
        doAnswer(invocation -> {
            opening.countDown();
            assertTrue(opened.await(5, TimeUnit.SECONDS));
            return null;
        }).when(mergedProject).open();
        whenMerging(MergeOpMode.SEND).thenReturn(MergeResult.builder().build());
        launcher.start(project, OTHER, MergeOpMode.SEND, false);
        runQueued();
        var first = CompletableFuture.runAsync(launcher::settleFinished);
        assertTrue(opening.await(5, TimeUnit.SECONDS));

        var read = new CompletableFuture<MergeResult>();
        var second = new Thread(() -> read.complete(launcher.result(project).orElseThrow()));
        second.start();
        var deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (second.getState() != Thread.State.BLOCKED) {
            assertTrue(second.isAlive() && System.nanoTime() < deadline, "The second request did not wait");
            Thread.onSpinWait();
        }
        opened.countDown();

        assertEquals(MergeResultStatus.SUCCESS, read.get(5, TimeUnit.SECONDS).status());
        first.get(5, TimeUnit.SECONDS);
        verify(studio, times(1)).reset();
    }

    @Test
    void findsNoMergeToCancel() throws Exception {
        whenMerging(MergeOpMode.SEND).thenReturn(MergeResult.builder().build());

        assertFalse(launcher.cancel(project));

        launcher.start(project, OTHER, MergeOpMode.SEND, false);
        runQueued();
        // An ended merge has nothing left to stop.
        assertFalse(launcher.cancel(project));
        assertEquals(MergeResultStatus.SUCCESS, launcher.result(project).orElseThrow().status());
    }

    @Test
    void tellsWhichProjectIsBeingMerged() throws Exception {
        var other = otherProject();
        // A project is resolved only while a merge of the client runs.
        assertFalse(launcher.isRunning(() -> fail("No merge runs, so no project is resolved")));

        launcher.start(project, OTHER, MergeOpMode.SEND, false);

        assertTrue(launcher.isRunning(() -> project));
        assertFalse(launcher.isRunning(() -> other));
    }

    @Test
    void findsNoMergeOfAnotherProject() throws Exception {
        launcher.start(project, OTHER, MergeOpMode.SEND, false);
        var other = otherProject();

        assertThrows(NotFoundException.class, () -> launcher.result(other));
    }

    @Test
    void findsNoMergeBeforeOneIsStarted() {

        assertThrows(NotFoundException.class, () -> launcher.result(project));
    }
}
