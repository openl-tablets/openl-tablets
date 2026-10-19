package org.openl.studio.projects.service.merge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.openl.rules.lock.LockInfo;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.AProjectArtefact;
import org.openl.rules.project.abstraction.LockEngine;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.repository.api.BranchRepository;
import org.openl.rules.repository.api.FeaturesBuilder;
import org.openl.rules.repository.api.MergeOutcome;
import org.openl.rules.repository.api.RepositoryDelegate;
import org.openl.rules.repository.api.UserInfo;
import org.openl.rules.repository.git.MergeConflictDetails;
import org.openl.rules.repository.git.MergeConflictException;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.workspace.WorkspaceUser;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.security.acl.repository.RepositoryAclService;
import org.openl.studio.audit.model.AuditAction;
import org.openl.studio.audit.model.AuditEvent;
import org.openl.studio.audit.service.ProjectAuditor;
import org.openl.studio.common.exception.ConflictException;
import org.openl.studio.projects.model.merge.CheckMergeStatus;
import org.openl.studio.projects.model.merge.MergeBlockedBy;
import org.openl.studio.projects.model.merge.MergeOpMode;
import org.openl.studio.projects.model.merge.MergeResultStatus;
import org.openl.studio.projects.model.merge.MergeScopeView;
import org.openl.studio.projects.model.merge.MergedChanges;
import org.openl.studio.projects.service.protection.ProtectedBranchBypassService;
import org.openl.studio.projects.validator.ProjectStateValidator;

/**
 * Where two branches stand is answered for everyone who may read the project. Whether this user may merge
 * them is reported next to the answer, never in place of it.
 */
class ProjectsMergeServiceCheckTest {

    private final List<AuditEvent> events = new ArrayList<>();

    private static final String CURRENT = "dev";
    private static final String OTHER = "master";

    private RulesProject project;
    private BranchRepository repository;
    private LockEngine lockEngine;
    private ProtectedBranchBypassService bypassService;
    private UserWorkspace userWorkspace;
    private MergeChangesService mergeChangesService;
    private MergedWorkbookFormatting mergedWorkbookFormatting;
    private WebStudio studio;
    private ProjectsMergeServiceImpl service;

    @BeforeEach
    void init() throws IOException {
        repository = mock(BranchRepository.class);
        when(repository.getId()).thenReturn("design");
        when(repository.branchExists(OTHER)).thenReturn(true);
        when(repository.supports()).thenReturn(new FeaturesBuilder(repository).setBranches(true).build());

        project = mock(RulesProject.class);
        when(project.getDesignRepository()).thenReturn(repository);
        when(project.getBranch()).thenReturn(CURRENT);
        lenient().when(project.getRealPath()).thenReturn("DESIGN/rules/P1");
        when(project.getArtefacts()).thenAnswer(invocation -> List.of(mock(AProjectArtefact.class)));

        var aclService = mock(RepositoryAclService.class);
        when(aclService.isGranted(any(AProjectArtefact.class), anyList())).thenReturn(true);

        var stateValidator = mock(ProjectStateValidator.class);
        when(stateValidator.canMerge(project)).thenReturn(true);

        lockEngine = mock(LockEngine.class);
        lenient().when(lockEngine.getLockInfo("design", CURRENT, "DESIGN/rules/P1")).thenReturn(LockInfo.NO_LOCK);
        lenient().when(lockEngine.getLockInfo("design", OTHER, "DESIGN/rules/P1")).thenReturn(LockInfo.NO_LOCK);
        userWorkspace = mock(UserWorkspace.class);
        lenient().when(userWorkspace.getProjectsLockEngine()).thenReturn(lockEngine);

        bypassService = mock(ProtectedBranchBypassService.class);

        mergeChangesService = mock(MergeChangesService.class);
        mergedWorkbookFormatting = mock(MergedWorkbookFormatting.class);
        lenient().when(mergedWorkbookFormatting.formatCommitted(any(), any(), any(), any()))
                .thenAnswer(invocation -> invocation.getArgument(2));
        studio = mock(WebStudio.class);
        service = new ProjectsMergeServiceImpl(stateValidator,
                aclService,
                bypassService,
                mergeChangesService,
                mergedWorkbookFormatting,
                new ProjectAuditor(event -> events.add((AuditEvent) event))) {
            @Override
            public UserWorkspace getUserWorkspace() {
                return userWorkspace;
            }
        };
    }

    @Test
    void anUnprotectedTargetTakesTheMerge() throws IOException {
        when(repository.isMergedInto(CURRENT, OTHER)).thenReturn(false);

        var result = service.checkMerge(project, OTHER, MergeOpMode.SEND);

        assertEquals(CheckMergeStatus.MERGEABLE, result.status());
        assertTrue(result.canMerge());
        assertNull(result.blockedBy());
    }

    @Test
    void aMergeableCheckTellsWhatTheMergeBringsIn() throws IOException {
        when(repository.isMergedInto(OTHER, CURRENT)).thenReturn(false);
        var scope = new MergeScopeView(List.of(), 2);
        when(mergeChangesService.scopeOf(project, repository, OTHER, CURRENT)).thenReturn(scope);

        var result = service.checkMerge(project, OTHER, MergeOpMode.RECEIVE);

        assertEquals(scope, result.scope());
    }

    @Test
    void anUpToDateCheckTellsNothingTheMergeWouldBringIn() throws IOException {
        when(repository.isMergedInto(OTHER, CURRENT)).thenReturn(true);

        var result = service.checkMerge(project, OTHER, MergeOpMode.RECEIVE);

        assertNull(result.scope());
        verify(mergeChangesService, never()).scopeOf(any(), any(), any(), any());
    }

    @Test
    void aProtectedTargetStillReportsTheDifference() throws IOException {
        when(repository.isMergedInto(CURRENT, OTHER)).thenReturn(false);
        when(repository.isBranchProtected(OTHER)).thenReturn(true);

        var result = service.checkMerge(project, OTHER, MergeOpMode.SEND);

        assertEquals(CheckMergeStatus.MERGEABLE, result.status());
        assertEquals(MergeBlockedBy.PROTECTED_BRANCH, result.blockedBy());
        assertFalse(result.canMerge());
    }

    @Test
    void aProtectedTargetAsksTheEligibleUserForABypass() throws IOException {
        when(repository.isMergedInto(CURRENT, OTHER)).thenReturn(true);
        when(repository.isBranchProtected(OTHER)).thenReturn(true);
        when(bypassService.isBypassEligible(project)).thenReturn(true);

        var result = service.checkMerge(project, OTHER, MergeOpMode.SEND);

        assertEquals(CheckMergeStatus.UP2DATE, result.status());
        assertEquals(MergeBlockedBy.BYPASS_REQUIRED, result.blockedBy());
    }

    @Test
    void takingAProtectedBranchIntoYourOwnIsAnOrdinaryMerge() throws IOException {
        when(repository.isMergedInto(OTHER, CURRENT)).thenReturn(false);
        when(repository.isBranchProtected(OTHER)).thenReturn(true);

        var result = service.checkMerge(project, OTHER, MergeOpMode.RECEIVE);

        assertEquals(OTHER, result.sourceBranch());
        assertEquals(CURRENT, result.targetBranch());
        assertTrue(result.canMerge());
        assertNull(result.blockedBy());
    }

    @Test
    void aLockedTargetBlocksTheMerge() throws IOException {
        when(repository.isMergedInto(CURRENT, OTHER)).thenReturn(false);
        var lockInfo = mock(LockInfo.class);
        when(lockInfo.isLocked()).thenReturn(true);
        when(lockEngine.getLockInfo("design", OTHER, "DESIGN/rules/P1")).thenReturn(lockInfo);

        var result = service.checkMerge(project, OTHER, MergeOpMode.SEND);

        assertEquals(MergeBlockedBy.LOCKED, result.blockedBy());
        assertFalse(result.canMerge());
    }

    @Test
    void aProjectScopedUserChecksTheRawBranchRepositoryAfterAclValidation() throws IOException {
        var securedRepository = mock(BranchRepository.class, withSettings().extraInterfaces(RepositoryDelegate.class));
        when(((RepositoryDelegate) securedRepository).getOriginal()).thenReturn(repository);
        when(project.getDesignRepository()).thenReturn(securedRepository);
        when(repository.isMergedInto(CURRENT, OTHER)).thenReturn(false);

        var result = service.checkMerge(project, OTHER, MergeOpMode.SEND);

        assertEquals(CheckMergeStatus.MERGEABLE, result.status());
        verify(repository).isMergedInto(CURRENT, OTHER);
    }

    @Test
    void aSuccessfulMergeWaitsUntilTheTargetBranchIsPublished() throws IOException {
        var designTimeRepository = givenMergeInto(OTHER);
        when(designTimeRepository.refreshBranch("design", OTHER))
                .thenReturn(CompletableFuture.completedFuture(null));
        when(repository.getId()).thenReturn("design");

        var result = service.merge(project, CURRENT, OTHER, MergeOpMode.SEND, userWorkspace, studio);

        assertEquals(MergeResultStatus.SUCCESS, result.status());
        verify(designTimeRepository).refreshBranch("design", OTHER);
        var merged = events.getFirst();
        assertEquals(AuditAction.BRANCH_MERGED, merged.action());
        assertEquals("design", merged.target());
        assertEquals(Map.of("branch", OTHER, "sourceBranch", CURRENT), merged.details());
    }

    @Test
    void aMergeIntoAProtectedBranchRecordsTheBypassOnceItIsMerged() throws IOException {
        var designTimeRepository = givenMergeInto(OTHER);
        when(designTimeRepository.refreshBranch("design", OTHER))
                .thenReturn(CompletableFuture.completedFuture(null));
        when(repository.getId()).thenReturn("design");
        when(repository.isBranchProtected(OTHER)).thenReturn(true);

        service.merge(project, CURRENT, OTHER, MergeOpMode.SEND, userWorkspace, studio);

        assertEquals(List.of(AuditAction.BRANCH_MERGED, AuditAction.PROTECTION_BYPASSED),
                events.stream().map(AuditEvent::action).toList());
    }

    @Test
    void aConflictingMergeIntoAProtectedBranchRecordsNoBypass() throws IOException {
        givenMergeInto(CURRENT);
        when(repository.isBranchProtected(CURRENT)).thenReturn(true);
        var targetRepository = repository.forBranch(CURRENT);
        doThrow(new MergeConflictException(mock(MergeConflictDetails.class))).when(targetRepository)
                .merge(eq(OTHER), any(), any());

        service.merge(project, CURRENT, OTHER, MergeOpMode.RECEIVE, userWorkspace, studio);

        assertTrue(events.isEmpty());
    }

    /**
     * The merge is committed before the index is asked to publish it. Reporting a failure would have the user merge
     * again a branch that has no changes left to merge — see EPBDS-16541.
     */
    @Test
    void anIndexThatDoesNotPublishTheMergeLeavesItASuccess() throws IOException {
        var designTimeRepository = givenMergeInto(OTHER);
        when(designTimeRepository.refreshBranch("design", OTHER))
                .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("index is busy")));

        var result = service.merge(project, CURRENT, OTHER, MergeOpMode.SEND, userWorkspace, studio);

        assertEquals(MergeResultStatus.SUCCESS, result.status());
        // An index that has not published the merge would tell the projects of the branch as they were before it.
        assertNull(result.changes());
        verify(mergeChangesService, never()).changesOf(any(), any(), any(), any(), any());
    }

    /** A merge may wait for a thread: a branch another user has taken up meanwhile refuses it when it starts. */
    @Test
    void aMergeIntoABranchLockedMeanwhileIsRefused() throws IOException {
        var lockInfo = mock(LockInfo.class);
        when(lockInfo.isLocked()).thenReturn(true);
        when(lockEngine.getLockInfo("design", OTHER, "DESIGN/rules/P1")).thenReturn(lockInfo);

        assertThrows(ConflictException.class,
                () -> service.merge(project, CURRENT, OTHER, MergeOpMode.SEND, userWorkspace, studio));

        verify(repository, never()).forBranch(any());
    }

    @Test
    void aMergeOfBranchesMergedMeanwhileIsRefused() throws IOException {
        when(repository.isMergedInto(CURRENT, OTHER)).thenReturn(true);

        assertThrows(ConflictException.class,
                () -> service.merge(project, CURRENT, OTHER, MergeOpMode.SEND, userWorkspace, studio));

        verify(repository, never()).forBranch(any());
    }

    @Test
    void aSuccessfulMergeKeepsWhatItChangedAgainstTheProjectsOfTheTargetBefore() throws IOException {
        var designTimeRepository = givenMergeInto(OTHER);
        when(designTimeRepository.refreshBranch("design", OTHER))
                .thenReturn(CompletableFuture.completedFuture(null));
        // The projects of the target are taken before the merge; the ones after it are read once it is published.
        var before = List.of(mock(AProject.class));
        when(mergeChangesService.projectsBefore("design", OTHER)).thenReturn(before);
        var outcome = new MergeOutcome("b1", "a1", List.of(), List.of());
        when(repository.forBranch(OTHER).merge(eq(CURRENT), any(), any())).thenReturn(outcome);

        var changes = mock(MergedChanges.class);
        when(mergeChangesService.changesOf("design", CURRENT, OTHER, outcome, before)).thenReturn(changes);

        var result = service.merge(project, CURRENT, OTHER, MergeOpMode.SEND, userWorkspace, studio);

        assertEquals(changes, result.changes());
    }

    /** Projects of the target that cannot be read before the merge leave it merged, only without what it changed. */
    @Test
    void aMergeWhoseProjectsBeforeCannotBeReadIsMergedAllTheSame() throws IOException {
        var designTimeRepository = givenMergeInto(OTHER);
        when(designTimeRepository.refreshBranch("design", OTHER))
                .thenReturn(CompletableFuture.completedFuture(null));
        when(mergeChangesService.projectsBefore("design", OTHER)).thenReturn(null);

        var result = service.merge(project, CURRENT, OTHER, MergeOpMode.SEND, userWorkspace, studio);

        assertEquals(MergeResultStatus.SUCCESS, result.status());
        assertNull(result.changes());
        verify(repository.forBranch(OTHER)).merge(eq(CURRENT), any(), any());
    }

    /** The workbooks a merge combines are formatted once it is committed, and what it changed counts them. */
    @Test
    void aMergeFormatsTheWorkbooksItCombinesOnceItIsCommitted() throws IOException {
        var designTimeRepository = givenMergeInto(OTHER);
        when(designTimeRepository.refreshBranch("design", OTHER))
                .thenReturn(CompletableFuture.completedFuture(null));
        var before = List.of(mock(AProject.class));
        when(mergeChangesService.projectsBefore("design", OTHER)).thenReturn(before);
        var merged = new MergeOutcome("b1", "a1", List.of(), List.of());
        when(repository.forBranch(OTHER).merge(eq(CURRENT), any(), any())).thenReturn(merged);
        var formatted = new MergeOutcome("b1", "a2", List.of(), List.of());
        var author = userWorkspace.getUser().getUserInfo();
        when(mergedWorkbookFormatting.formatCommitted(project,
                repository,
                merged,
                new MergedWorkbookFormatting.CommittedMerge(CURRENT, OTHER, author, studio))).thenReturn(formatted);
        var changes = mock(MergedChanges.class);
        when(mergeChangesService.changesOf("design", CURRENT, OTHER, formatted, before)).thenReturn(changes);

        var result = service.merge(project, CURRENT, OTHER, MergeOpMode.SEND, userWorkspace, studio);

        assertEquals(changes, result.changes());
    }

    @Test
    void aConflictIsReportedWithTheBranchesOfTheMerge() throws IOException {
        givenMergeInto(CURRENT);
        var targetRepository = repository.forBranch(CURRENT);
        var details = mock(MergeConflictDetails.class);
        doThrow(new MergeConflictException(details)).when(targetRepository).merge(eq(OTHER), any(), any());

        var result = service.merge(project, CURRENT, OTHER, MergeOpMode.RECEIVE, userWorkspace, studio);

        assertEquals(MergeResultStatus.CONFLICTS, result.status());
        assertEquals(OTHER, result.conflictInfo().mergeBranchFrom());
        assertEquals(CURRENT, result.conflictInfo().mergeBranchTo());
        assertEquals(details, result.conflictInfo().details());
    }

    /** The repository of the branch a merge commits into, for the user of the workspace. */
    private DesignTimeRepository givenMergeInto(String branch) throws IOException {
        givenTarget(branch);
        var designTimeRepository = mock(DesignTimeRepository.class);
        when(userWorkspace.getDesignTimeRepository()).thenReturn(designTimeRepository);
        return designTimeRepository;
    }

    private BranchRepository givenTarget(String branch) throws IOException {
        var targetRepository = mock(BranchRepository.class);
        when(repository.forBranch(branch)).thenReturn(targetRepository);
        lenient().when(targetRepository.merge(any(), any(), any()))
                .thenReturn(new MergeOutcome("before", "after", List.of(), List.of()));
        var user = mock(WorkspaceUser.class);
        var userInfo = mock(UserInfo.class);
        when(user.getUserInfo()).thenReturn(userInfo);
        when(userWorkspace.getUser()).thenReturn(user);
        return targetRepository;
    }
}
