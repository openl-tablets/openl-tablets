package org.openl.studio.projects.service.protection;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.acls.domain.BasePermission;

import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.repository.api.BranchRepository;
import org.openl.rules.rest.acl.service.AclProjectsHelper;
import org.openl.security.acl.repository.RepositoryAclService;
import org.openl.studio.common.exception.ForbiddenException;
import org.openl.studio.common.exception.ProtectedBranchBypassRequiredException;

class ProtectedBranchBypassServiceImplTest {

    private static final String BRANCH = "main";
    private static final String REPO_ID = "repo-1";

    private AclProjectsHelper aclProjectsHelper;
    private RepositoryAclService designRepositoryAclService;
    private BranchRepository repo;
    private RulesProject project;

    @BeforeEach
    void setUp() {
        aclProjectsHelper = mock(AclProjectsHelper.class);
        designRepositoryAclService = mock(RepositoryAclService.class);
        repo = mock(BranchRepository.class);
        project = mock(RulesProject.class);
        lenient().when(repo.isBranchProtected(anyString())).thenReturn(true);
    }

    private ProtectedBranchBypassServiceImpl service(boolean enabled) {
        return new ProtectedBranchBypassServiceImpl(aclProjectsHelper, designRepositoryAclService, enabled);
    }

    // --- isBypassEligible(project) ---

    @Test
    void isBypassEligible_settingDisabled_returnsFalse() {
        when(aclProjectsHelper.hasPermission(any(RulesProject.class), eq(BasePermission.ADMINISTRATION))).thenReturn(true);
        assertFalse(service(false).isBypassEligible(project));
    }

    @Test
    void isBypassEligible_settingEnabledNotManager_returnsFalse() {
        when(aclProjectsHelper.hasPermission(any(RulesProject.class), eq(BasePermission.ADMINISTRATION))).thenReturn(false);
        assertFalse(service(true).isBypassEligible(project));
    }

    @Test
    void isBypassEligible_settingEnabledManager_returnsTrue() {
        when(aclProjectsHelper.hasPermission(any(RulesProject.class), eq(BasePermission.ADMINISTRATION))).thenReturn(true);
        assertTrue(service(true).isBypassEligible(project));
    }

    @Test
    void isBypassEligible_nullProject_returnsFalse() {
        assertFalse(service(true).isBypassEligible((org.openl.rules.project.abstraction.AProject) null));
    }

    // --- isBypassEligible(repoId) ---

    @Test
    void isBypassEligibleRepoId_settingDisabled_returnsFalse() {
        when(designRepositoryAclService.isGranted(eq(REPO_ID), isNull(), eq(List.of(BasePermission.ADMINISTRATION)))).thenReturn(true);
        assertFalse(service(false).isBypassEligible(REPO_ID));
    }

    @Test
    void isBypassEligibleRepoId_settingEnabledNotManager_returnsFalse() {
        when(designRepositoryAclService.isGranted(eq(REPO_ID), isNull(), eq(List.of(BasePermission.ADMINISTRATION)))).thenReturn(false);
        assertFalse(service(true).isBypassEligible(REPO_ID));
    }

    @Test
    void isBypassEligibleRepoId_settingEnabledManager_returnsTrue() {
        when(designRepositoryAclService.isGranted(eq(REPO_ID), isNull(), eq(List.of(BasePermission.ADMINISTRATION)))).thenReturn(true);
        assertTrue(service(true).isBypassEligible(REPO_ID));
    }

    // --- requireBypassOrThrow(project) ---

    @Test
    void requireBypass_branchNotProtected_isNoOp() {
        when(repo.isBranchProtected(BRANCH)).thenReturn(false);
        assertDoesNotThrow(() -> service(true).requireBypassOrThrow(repo, BRANCH, project, false));
    }

    @Test
    void requireBypass_protectedNotEligible_throwsForbidden() {
        when(aclProjectsHelper.hasPermission(any(RulesProject.class), eq(BasePermission.ADMINISTRATION))).thenReturn(false);
        assertThrows(ForbiddenException.class,
                () -> service(true).requireBypassOrThrow(repo, BRANCH, project, true));
    }

    @Test
    void requireBypass_settingDisabledThrowsForbiddenEvenForManager() {
        when(aclProjectsHelper.hasPermission(any(RulesProject.class), eq(BasePermission.ADMINISTRATION))).thenReturn(true);
        assertThrows(ForbiddenException.class,
                () -> service(false).requireBypassOrThrow(repo, BRANCH, project, true));
    }

    @Test
    void requireBypass_protectedEligibleNoForce_throwsBypassRequired() {
        when(aclProjectsHelper.hasPermission(any(RulesProject.class), eq(BasePermission.ADMINISTRATION))).thenReturn(true);
        assertThrows(ProtectedBranchBypassRequiredException.class,
                () -> service(true).requireBypassOrThrow(repo, BRANCH, project, false));
    }

    @Test
    void requireBypass_protectedEligibleWithForce_isNoOp() {
        when(aclProjectsHelper.hasPermission(any(RulesProject.class), eq(BasePermission.ADMINISTRATION))).thenReturn(true);
        assertDoesNotThrow(() -> service(true).requireBypassOrThrow(repo, BRANCH, project, true));
    }

    // --- requireBypassOrThrow(repoId) ---

    @Test
    void requireBypassRepoId_protectedEligibleNoForce_throwsBypassRequired() {
        when(designRepositoryAclService.isGranted(eq(REPO_ID), isNull(), eq(List.of(BasePermission.ADMINISTRATION)))).thenReturn(true);
        assertThrows(ProtectedBranchBypassRequiredException.class,
                () -> service(true).requireBypassOrThrow(repo, BRANCH, REPO_ID, false));
    }

    @Test
    void requireBypassRepoId_protectedEligibleWithForce_isNoOp() {
        when(designRepositoryAclService.isGranted(eq(REPO_ID), isNull(), eq(List.of(BasePermission.ADMINISTRATION)))).thenReturn(true);
        assertDoesNotThrow(() -> service(true).requireBypassOrThrow(repo, BRANCH, REPO_ID, true));
    }

    @Test
    void requireBypassRepoId_protectedNotEligible_throwsForbidden() {
        when(designRepositoryAclService.isGranted(eq(REPO_ID), isNull(), eq(List.of(BasePermission.ADMINISTRATION)))).thenReturn(false);
        assertThrows(ForbiddenException.class,
                () -> service(true).requireBypassOrThrow(repo, BRANCH, REPO_ID, true));
    }
}
