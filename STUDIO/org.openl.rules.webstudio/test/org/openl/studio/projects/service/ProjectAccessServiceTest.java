package org.openl.studio.projects.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.acls.model.Permission;

import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.project.abstraction.UserWorkspaceProject;
import org.openl.rules.repository.api.BranchRepository;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.rest.acl.service.AclProjectsHelper;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.studio.projects.validator.ProjectStateValidator;
import org.openl.studio.repositories.service.DeploymentRepositoryService;
import org.openl.studio.repositories.service.DesignTimeRepositoryService;

class ProjectAccessServiceTest {

    private AclProjectsHelper aclProjectsHelper;
    private ProjectStateValidator stateValidator;
    private DeploymentRepositoryService deploymentRepositoryService;
    private DesignTimeRepositoryService designTimeRepositoryService;
    private ProjectListingContext listingContext;
    private DesignTimeRepository designTimeRepository;
    private UserWorkspaceProject project;
    private ProjectAccessService service;

    @BeforeEach
    void setUp() {
        aclProjectsHelper = mock(AclProjectsHelper.class);
        stateValidator = mock(ProjectStateValidator.class);
        deploymentRepositoryService = mock(DeploymentRepositoryService.class);
        designTimeRepositoryService = mock(DesignTimeRepositoryService.class);
        listingContext = mock(ProjectListingContext.class);
        designTimeRepository = mock(DesignTimeRepository.class);
        project = mock(UserWorkspaceProject.class);
        when(project.getRepository()).thenReturn(mock(Repository.class));
        service = new ProjectAccessService(aclProjectsHelper, stateValidator, deploymentRepositoryService,
                designTimeRepositoryService, listingContext, designTimeRepository);
    }

    private void grant(Permission... permissions) {
        for (Permission permission : permissions) {
            when(aclProjectsHelper.hasPermission(project, permission)).thenReturn(true);
        }
    }

    @Test
    void read_enables_viewing_but_not_management() {
        grant(BasePermission.READ);

        var caps = service.computeCapabilities(project);

        assertEquals(Boolean.TRUE, caps.canCompare());
        assertEquals(Boolean.TRUE, caps.canViewHistory());
        assertNull(caps.canManage());
    }

    @Test
    void project_edits_require_write_opened_for_editing_and_modifiable_state() {
        grant(BasePermission.WRITE);
        when(stateValidator.canModify(project)).thenReturn(true);

        // Not opened for editing yet — no content modification even though the state allows it.
        assertNull(service.computeCapabilities(project).project().canWrite());

        when(project.isOpenedForEditing()).thenReturn(true);

        var caps = service.computeCapabilities(project);
        assertEquals(Boolean.TRUE, caps.project().canWrite());
    }

    @Test
    void save_requires_write_and_savable_state() {
        grant(BasePermission.WRITE);
        when(stateValidator.canSave(project)).thenReturn(true);

        assertEquals(Boolean.TRUE, service.computeCapabilities(project).canSave());
    }

    @Test
    void open_requires_read_and_openable_state() {
        grant(BasePermission.READ);
        when(stateValidator.canOpen(project)).thenReturn(true);

        assertEquals(Boolean.TRUE, service.computeCapabilities(project).canOpen());
    }

    @Test
    void delete_project_requires_delete_and_state() {
        grant(BasePermission.DELETE);
        when(stateValidator.canDelete(project)).thenReturn(true);

        var caps = service.computeCapabilities(project);
        assertEquals(Boolean.TRUE, caps.project().canDelete());
    }

    @Test
    void delete_project_denied_without_delete_permission() {
        when(stateValidator.canDelete(project)).thenReturn(true);

        assertNull(service.computeCapabilities(project).project().canDelete());
    }

    @Test
    void manage_and_unlock_require_administration() {
        grant(BasePermission.ADMINISTRATION);
        when(project.isLocked()).thenReturn(true);

        var caps = service.computeCapabilities(project);
        assertEquals(Boolean.TRUE, caps.canManage());
        assertEquals(Boolean.TRUE, caps.canUnlock());
    }

    @Test
    void deploy_requires_committed_state_and_a_writable_production_repository() {
        when(stateValidator.canDeploy(project)).thenReturn(true);
        // The user has WRITE on some production repository (legacy canRedeployProject gate).
        when(listingContext.canDeployToAnyRepository(any())).thenReturn(true);
        assertEquals(Boolean.TRUE, service.computeCapabilities(project).canDeploy());

        // No writable production repository — the button is hidden even though the state allows it.
        when(listingContext.canDeployToAnyRepository(any())).thenReturn(false);
        assertNull(service.computeCapabilities(project).canDeploy());

        // A local-only project is never deployable, regardless of state.
        when(listingContext.canDeployToAnyRepository(any())).thenReturn(true);
        when(project.isLocalOnly()).thenReturn(true);
        assertNull(service.computeCapabilities(project).canDeploy());
    }

    @Test
    void copy_requires_a_creatable_target_repository_and_a_non_local_project() {
        // A copy is created in whatever repository the user picks, so it mirrors the "can create in any
        // repository" check (which itself honours branch protection), not the source repository.
        when(listingContext.canCreateInAnyRepository(any())).thenReturn(true);
        assertEquals(Boolean.TRUE, service.computeCapabilities(project).canCopy());

        // No repository accepts a new project — nowhere to copy to, so the action is hidden.
        when(listingContext.canCreateInAnyRepository(any())).thenReturn(false);
        assertNull(service.computeCapabilities(project).canCopy());

        // A local-only project is never copyable, regardless of repository availability.
        when(listingContext.canCreateInAnyRepository(any())).thenReturn(true);
        when(project.isLocalOnly()).thenReturn(true);
        assertNull(service.computeCapabilities(project).canCopy());
    }

    @Test
    void deleting_the_last_branch_of_a_project_takes_the_permission_to_delete_the_project() {
        var rulesProject = mock(RulesProject.class);
        var repository = mock(BranchRepository.class);
        when(repository.getId()).thenReturn("design");
        when(rulesProject.getDesignRepository()).thenReturn(repository);
        when(rulesProject.getDesignProjectName()).thenReturn("Rates");
        when(rulesProject.getBranch()).thenReturn("feature");
        when(aclProjectsHelper.hasPermission(rulesProject, BasePermission.WRITE)).thenReturn(true);
        when(stateValidator.canDeleteBranch(rulesProject)).thenReturn(true);

        // Another branch still holds the project, so deleting this one only deletes a branch.
        when(designTimeRepository.isLastProjectBranch("design", "Rates", "feature")).thenReturn(false);
        assertEquals(Boolean.TRUE, service.computeCapabilities(rulesProject).canDeleteBranch());

        // It is the last branch: the deletion removes the project, which write access alone must not do.
        when(designTimeRepository.isLastProjectBranch("design", "Rates", "feature")).thenReturn(true);
        assertNull(service.computeCapabilities(rulesProject).canDeleteBranch());

        when(aclProjectsHelper.hasPermission(rulesProject, BasePermission.DELETE)).thenReturn(true);
        assertEquals(Boolean.TRUE, service.computeCapabilities(rulesProject).canDeleteBranch());

        // The branch itself must be deletable in the first place — the base branch never is.
        when(stateValidator.canDeleteBranch(rulesProject)).thenReturn(false);
        assertNull(service.computeCapabilities(rulesProject).canDeleteBranch());
    }

    // The Copy dialog offers a new project, a new branch, or both, so either right opens it — and it answers
    // the same as the capabilities it is derived from.
    @Test
    void copying_needs_either_a_repository_to_create_in_or_a_branch_to_cut() {
        assertFalse(service.canCopyOrBranch(project));

        when(listingContext.canCreateInAnyRepository(any())).thenReturn(true);
        assertTrue(service.canCopyOrBranch(project));
        assertEquals(Boolean.TRUE, service.computeCapabilities(project).canCopy());

        // No repository to create in, but the project may be branched instead.
        when(listingContext.canCreateInAnyRepository(any())).thenReturn(false);
        when(project.isSupportsBranches()).thenReturn(true);
        grant(BasePermission.WRITE);
        assertTrue(service.canCopyOrBranch(project));
        assertEquals(Boolean.TRUE, service.computeCapabilities(project).canManageBranches());
    }

    @Test
    void a_local_only_project_is_neither_copied_nor_branched() {
        when(project.isLocalOnly()).thenReturn(true);
        when(listingContext.canCreateInAnyRepository(any())).thenReturn(true);
        when(project.isSupportsBranches()).thenReturn(true);
        grant(BasePermission.WRITE);

        assertFalse(service.canCopyOrBranch(project));
        assertNull(service.computeCapabilities(project).canCopy());
        assertNull(service.computeCapabilities(project).canManageBranches());
    }
}
