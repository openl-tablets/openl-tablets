package org.openl.studio.projects.service;

import static org.openl.studio.common.model.Capabilities.flag;

import lombok.RequiredArgsConstructor;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.stereotype.Service;

import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.project.abstraction.UserWorkspaceProject;
import org.openl.rules.rest.acl.service.AclProjectsHelper;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.studio.common.model.Capabilities;
import org.openl.studio.projects.model.ProjectCapabilities;
import org.openl.studio.projects.validator.ProjectStateValidator;
import org.openl.studio.repositories.service.DeploymentRepositoryService;
import org.openl.studio.repositories.service.DesignTimeRepositoryService;

/**
 * Computes the current user's capabilities on a project for UI action gating.
 *
 * <p>Each capability is the conjunction of an effective permission — probed through
 * {@link AclProjectsHelper}, which honours ACL inheritance, the administrator override, local-only
 * projects and the configuration properties that can disable an operation — and the project state,
 * evaluated by {@link ProjectStateValidator}, the same validator the operations enforce with.
 *
 * <p>The result is advisory: it lets the UI show or hide controls. Every operation is still enforced
 * server-side. A capability that is not granted is {@code null} (omitted) rather than {@code false}.
 */
@Service
@RequiredArgsConstructor
public class ProjectAccessService {

    private final AclProjectsHelper aclProjectsHelper;
    private final ProjectStateValidator projectStateValidator;
    private final DeploymentRepositoryService deploymentRepositoryService;
    private final DesignTimeRepositoryService designTimeRepositoryService;
    private final ProjectListingContext listingContext;
    private final DesignTimeRepository designTimeRepository;

    public ProjectCapabilities computeCapabilities(AProject project) {
        var read = aclProjectsHelper.hasPermission(project, BasePermission.READ);
        var write = aclProjectsHelper.hasPermission(project, BasePermission.WRITE);
        var delete = aclProjectsHelper.hasPermission(project, BasePermission.DELETE);
        var administer = aclProjectsHelper.hasPermission(project, BasePermission.ADMINISTRATION);
        if (!(project instanceof UserWorkspaceProject workspaceProject)) {
            return ProjectCapabilities.builder().build();
        }
        var localOnly = workspaceProject.isLocalOnly();
        // A local-only project is itself the working copy, so it is always editable; a committed project
        // must be opened for editing first. canModify folds in the branch-protection and lock state.
        var editable = projectStateValidator.canModify(workspaceProject)
                && (localOnly || workspaceProject.isOpenedForEditing());
        // Compare, view-history and export are all "read a shared (non-local) project".
        var readShared = read && !localOnly;
        return ProjectCapabilities.builder()
                .project(Capabilities.builder()
                        .canWrite(flag(write && editable))
                        .canDelete(flag(delete && projectStateValidator.canDelete(workspaceProject)))
                        .build())
                .canOpen(flag(read && projectStateValidator.canOpen(workspaceProject)))
                .canClose(flag(projectStateValidator.canClose(workspaceProject)))
                .canSave(flag(write && projectStateValidator.canSave(workspaceProject)))
                .canUnlock(flag(administer && workspaceProject.isLocked() && !workspaceProject.isLockedByMe()))
                .canDeploy(flag(!localOnly && projectStateValidator.canDeploy(workspaceProject)
                        && listingContext.canDeployToAnyRepository(deploymentRepositoryService::canDeployToAnyRepository)))
                .canCompare(flag(readShared))
                .canViewHistory(flag(readShared))
                .canManage(flag(administer && !localOnly))
                // Copy creates a new project in a repository the user picks, so it mirrors the copy dialog's
                // repository list — not just the source repository.
                .canCopy(flag(!localOnly && canCreateSomewhere()))
                .canManageBranches(flag(!localOnly && canBranch(workspaceProject, write)))
                .canDeleteBranch(flag(canDeleteBranch(workspaceProject, write, delete)))
                .canExport(flag(readShared))
                .build();
    }

    /**
     * Whether the Copy dialog has anything to offer for this project.
     *
     * <p>A copy is either a new project in some repository or a new branch of this one, so either right is
     * enough. A local-only project is neither copied nor branched.
     *
     * <p>Answering this alone, rather than through {@link #computeCapabilities(AProject)}, keeps the two
     * flags it reads from costing the eleven it does not — this is asked while a page renders.
     */
    public boolean canCopyOrBranch(UserWorkspaceProject project) {
        if (project.isLocalOnly()) {
            return false;
        }
        // One permission probe on the project, so it is asked before the repository scan: a page evaluates
        // this on every render, and whoever is editing the project usually answers it here.
        return canBranch(project, aclProjectsHelper.hasPermission(project, BasePermission.WRITE))
                || canCreateSomewhere();
    }

    /** Whether a project may be created in any repository at all — the target list a copy picks from. */
    private boolean canCreateSomewhere() {
        return listingContext.canCreateInAnyRepository(designTimeRepositoryService::canCreateInAnyRepository);
    }

    /**
     * Whether branches of this project may be managed. Branching is governed by write access to the project
     * itself, not by the permission to create a project, so a user who may not create projects can still
     * branch. The per-artefact check, the branch protection and the base-branch rule are enforced when the
     * operation runs.
     */
    private boolean canBranch(UserWorkspaceProject project, boolean write) {
        return project.isSupportsBranches() && write;
    }

    /**
     * Whether the branch the project sits on can be deleted. The base branch and a protected branch without the
     * bypass right are refused when the deletion runs, so the action is not offered for them either. Deleting the
     * only branch that holds the project deletes the project, which takes the right to delete a project rather
     * than the right to manage branches.
     */
    private boolean canDeleteBranch(UserWorkspaceProject project, boolean write, boolean delete) {
        if (!write || !(project instanceof RulesProject rulesProject)
                || !projectStateValidator.canDeleteBranch(rulesProject)) {
            return false;
        }
        return delete || !designTimeRepository.isLastProjectBranch(rulesProject.getDesignRepository().getId(),
                rulesProject.getDesignProjectName(),
                rulesProject.getBranch());
    }
}
