package org.openl.rules.rest.project;

import org.openl.rules.project.abstraction.UserWorkspaceProject;
import org.springframework.stereotype.Component;

/**
 * Project state validator implementation
 */
@Component
public class ProjectStateValidatorImpl implements ProjectStateValidator {

    @Override
    public boolean canClose(UserWorkspaceProject project) {
        if (project != null) {
            return !project.isLocalOnly() && project.isOpened();
        } else {
            return false;
        }
    }

    @Override
    public boolean canOpen(UserWorkspaceProject project) {
        return project != null && !project.isLocalOnly() && !project.isOpenedForEditing() && !project.isOpened();
    }

    @Override
    public boolean canDeploy(UserWorkspaceProject project) {
        return !project.isModified();
    }
}
