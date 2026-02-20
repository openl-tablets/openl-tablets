package org.openl.studio.projects.validator;

import java.io.IOException;

import org.springframework.stereotype.Component;

import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.project.abstraction.UserWorkspaceProject;
import org.openl.rules.repository.api.BranchRepository;
import org.openl.rules.repository.api.Repository;

/**
 * Project state validator implementation
 */
@Component
public class ProjectStateValidatorImpl implements ProjectStateValidator {

    @Override
    public boolean canSave(UserWorkspaceProject project) {
        return project != null && project.isModified() && isEditableProject(project);
    }

    private boolean isEditableProject(UserWorkspaceProject project) {
        if (isCurrentBranchProtected(project)) {
            return false;
        }
        return project.isLocalOnly() || !project.isLocked() || project.isOpenedForEditing();
    }

    private boolean isCurrentBranchProtected(UserWorkspaceProject project) {
        if (project != null && !project.isLocalOnly()) {
            Repository repo = project.getDesignRepository();
            return repo.supports().branches() && ((BranchRepository) repo).isBranchProtected(project.getBranch());
        }
        return false;
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
        if (project.getRepository().supports().branches() && project.getVersion() == null) {
            return false;
        }

        return !project.isOpened() && !project.isLocked() && !project.isLockedByMe();
    }

    @Override
    public boolean canErase(UserWorkspaceProject project) {
        return project != null && project.isDeleted();
    }

    @Override
    public boolean canMerge(RulesProject project) {
        if (project == null || !project.getDesignRepository().supports().branches() || project.isLocalOnly()) {
            return false;
        }

        try {
            if (project.isModified()) {
                return false;
            }
            var branches = ((BranchRepository) project.getDesignRepository()).getBranches(project.getDesignFolderName());
            return branches.size() >= 2;
        } catch (IOException e) {
            return false;
        }
    }
}
