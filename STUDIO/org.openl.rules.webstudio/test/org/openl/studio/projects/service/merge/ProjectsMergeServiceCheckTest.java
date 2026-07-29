package org.openl.studio.projects.service.merge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.openl.rules.lock.LockInfo;
import org.openl.rules.project.abstraction.AProjectArtefact;
import org.openl.rules.project.abstraction.LockEngine;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.repository.api.BranchRepository;
import org.openl.rules.repository.api.FeaturesBuilder;
import org.openl.rules.repository.api.RepositoryDelegate;
import org.openl.rules.repository.api.UserInfo;
import org.openl.rules.workspace.WorkspaceUser;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.security.acl.repository.RepositoryAclService;
import org.openl.studio.projects.model.merge.CheckMergeStatus;
import org.openl.studio.projects.model.merge.MergeBlockedBy;
import org.openl.studio.projects.model.merge.MergeOpMode;
import org.openl.studio.projects.service.protection.ProtectedBranchBypassService;
import org.openl.studio.projects.validator.ProjectStateValidator;

/**
 * Where two branches stand is answered for everyone who may read the project. Whether this user may merge
 * them is reported next to the answer, never in place of it.
 */
class ProjectsMergeServiceCheckTest {

    private static final String CURRENT = "dev";
    private static final String OTHER = "master";

    private RulesProject project;
    private BranchRepository repository;
    private LockEngine lockEngine;
    private ProtectedBranchBypassService bypassService;
    private UserWorkspace userWorkspace;
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

        service = new ProjectsMergeServiceImpl(stateValidator, aclService, bypassService) {
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
        var targetRepository = mock(BranchRepository.class);
        when(repository.forBranch(OTHER)).thenReturn(targetRepository);
        when(repository.isMergedInto(CURRENT, OTHER)).thenReturn(false);
        var user = mock(WorkspaceUser.class);
        when(user.getUserInfo()).thenReturn(mock(UserInfo.class));
        when(userWorkspace.getUser()).thenReturn(user);
        var designTimeRepository = mock(DesignTimeRepository.class);
        when(userWorkspace.getDesignTimeRepository()).thenReturn(designTimeRepository);
        when(designTimeRepository.refreshBranch("design", OTHER))
                .thenReturn(CompletableFuture.completedFuture(null));

        service.merge(project, OTHER, MergeOpMode.SEND, false);

        verify(designTimeRepository).refreshBranch("design", OTHER);
    }
}
