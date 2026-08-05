package org.openl.studio.projects.validator;

import java.io.IOException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.project.abstraction.UserWorkspaceProject;
import org.openl.rules.repository.api.BranchRepository;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.repository.api.RepositoryDelegate;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.studio.projects.service.protection.ProtectedBranchBypassService;

/**
 * Project state validator implementation
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ProjectStateValidatorImpl implements ProjectStateValidator {

    private final ProtectedBranchBypassService bypassService;
    private final DesignTimeRepository designTimeRepository;

    @Override
    public boolean canSave(UserWorkspaceProject project) {
        return project != null && project.isModified() && isEditableProject(project);
    }

    private boolean isEditableProject(UserWorkspaceProject project) {
        if (isCurrentBranchProtectionEnforced(project)) {
            return false;
        }
        return project.isLocalOnly() || !project.isLocked() || project.isOpenedForEditing();
    }

    private boolean isCurrentBranchProtectionEnforced(UserWorkspaceProject project) {
        if (project != null && !project.isLocalOnly()) {
            var repo = project.getDesignRepository();
            if (repo != null && repo.supports().branches()) {
                return bypassService.isProtectionEnforced(
                        (BranchRepository) repo, project.getBranch(), project);
            }
        }
        return false;
    }

    @Override
    public boolean canModify(UserWorkspaceProject project) {
        return project != null && isEditableProject(project);
    }

    @Override
    public boolean canClose(UserWorkspaceProject project) {
        if (project == null || project.isDeleted()) {
            return false;
        }
        return !project.isLocalOnly() && project.isOpened();
    }

    @Override
    public boolean canOpen(UserWorkspaceProject project) {
        if (project == null || project.isDeleted()) {
            return false;
        }
        return !project.isLocalOnly() && !project.isOpenedForEditing() && !project.isOpened();
    }

    @Override
    public boolean canDeploy(UserWorkspaceProject project) {
        if (project == null || project.isDeleted()) {
            return false;
        }
        return !project.isModified();
    }

    @Override
    public boolean canDelete(UserWorkspaceProject project) {
        if (project == null || project.isDeleted()) {
            return false;
        }
        if (project.isLocalOnly()) {
            // any user can delete own local project
            return true;
        }
        if (!canDeleteFromBranch(project)) {
            return false;
        }
        // An opened project is closed for all users during deletion, so only a lock held by another user blocks it.
        return !project.isLocked() || project.isLockedByMe();
    }

    private boolean canDeleteFromBranch(UserWorkspaceProject project) {
        var repo = project.getDesignRepository();
        if (!repo.supports().branches()) {
            return true;
        }
        if (isCurrentBranchProtectionEnforced(project)) {
            return false;
        }
        return project.getVersion() != null;
    }

    @Override
    public boolean canMerge(RulesProject project) {
        if (project == null || !project.getDesignRepository().supports().branches() || project.isLocalOnly()) {
            return false;
        }

        return !project.isModified() && hasMergeTarget(project);
    }

    @Override
    public boolean canDeleteBranch(RulesProject project) {
        if (project == null || project.isLocalOnly() || !project.isSupportsBranches()) {
            return false;
        }
        var branch = project.getBranch();
        var repository = (BranchRepository) project.getDesignRepository();
        if (branch == null || branch.equalsIgnoreCase(repository.getBaseBranch())) {
            return false;
        }
        return !isCurrentBranchProtectionEnforced(project);
    }

    @Override
    public boolean isLastProjectBranch(RulesProject project) {
        if (project == null || project.isLocalOnly() || !project.isSupportsBranches()) {
            return false;
        }
        var branch = project.getBranch();
        return branch != null && designTimeRepository
                .getBranchedProject(project.getDesignRepository().getId(), project.getDesignProjectName())
                .filter(branchedProject -> branchedProject.entries()
                        .keySet()
                        .stream()
                        .anyMatch(other -> !other.equalsIgnoreCase(branch)))
                .isEmpty();
    }

    private boolean hasMergeTarget(RulesProject project) {
        var repository = unwrap((BranchRepository) project.getDesignRepository());
        try {
            return repository.listBranches().stream().anyMatch(branch -> !branch.equals(project.getBranch()));
        } catch (IOException e) {
            log.debug("Cannot list merge targets for project '{}'.", project.getName(), e);
            return false;
        }
    }

    private static BranchRepository unwrap(BranchRepository repository) {
        Repository current = repository;
        while (current instanceof RepositoryDelegate delegate) {
            current = delegate.getOriginal();
        }
        return (BranchRepository) current;
    }
}
