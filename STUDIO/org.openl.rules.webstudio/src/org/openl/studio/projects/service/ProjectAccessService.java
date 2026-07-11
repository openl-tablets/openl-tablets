package org.openl.studio.projects.service;

import static org.openl.studio.common.model.Capabilities.flag;

import lombok.RequiredArgsConstructor;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.stereotype.Service;

import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.UserWorkspaceProject;
import org.openl.rules.rest.acl.service.AclProjectsHelper;
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

    public ProjectCapabilities computeCapabilities(AProject project) {
        boolean read = aclProjectsHelper.hasPermission(project, BasePermission.READ);
        boolean write = aclProjectsHelper.hasPermission(project, BasePermission.WRITE);
        boolean delete = aclProjectsHelper.hasPermission(project, BasePermission.DELETE);
        boolean administer = aclProjectsHelper.hasPermission(project, BasePermission.ADMINISTRATION);
        if (!(project instanceof UserWorkspaceProject workspaceProject)) {
            return ProjectCapabilities.builder().build();
        }
        boolean localOnly = workspaceProject.isLocalOnly();
        // A local-only project is itself the working copy, so it is always editable; a committed project
        // must be opened for editing first. canModify folds in the branch-protection and lock state.
        boolean editable = projectStateValidator.canModify(workspaceProject)
                && (localOnly || workspaceProject.isOpenedForEditing());
        // Compare, view-history and export are all "read a shared (non-local) project".
        boolean readShared = read && !localOnly;
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
                .canEditTags(flag(write && workspaceProject.isOpened()))
                .canManage(flag(administer && !localOnly))
                // Copy creates a new project in a repository the user picks, so it mirrors the copy dialog's
                // repository list: available when the user can create a project in any repository (permission
                // and, for branch repositories, an unprotected branch) — not just the source repository.
                .canCopy(flag(!localOnly && listingContext.canCreateInAnyRepository(
                        designTimeRepositoryService::canCreateInAnyRepository)))
                .canExport(flag(readShared))
                .build();
    }
}
