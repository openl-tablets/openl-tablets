package org.openl.studio.projects.validator;

import java.io.IOException;

import org.springframework.stereotype.Component;

import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.project.abstraction.UserWorkspaceProject;
import org.openl.rules.repository.api.BranchRepository;

/**
 * Project state validator implementation
 */
@Component
public class ProjectStateValidatorImpl implements ProjectStateValidator {

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

        if (!project.isLocalOnly() && project.getRepository().supports().branches() && project.getVersion() == null) {
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
