package org.openl.studio.projects.validator;

import org.springframework.stereotype.Component;

import org.openl.rules.project.abstraction.UserWorkspaceProject;

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
}
