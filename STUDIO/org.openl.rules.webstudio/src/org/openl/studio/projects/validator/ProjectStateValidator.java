package org.openl.studio.projects.validator;

import org.openl.rules.project.abstraction.UserWorkspaceProject;

/**
 * Project state validator
 */
public interface ProjectStateValidator {

    /**
     * Check if project can be closed
     *
     * @param project project
     * @return true or false
     */
    boolean canClose(UserWorkspaceProject project);

    /**
     * Check if project can be opened
     *
     * @param project project
     * @return true or false
     */
    boolean canOpen(UserWorkspaceProject project);

    /**
     * Check if project can be deployed
     *
     * @param project project
     * @return true or false
     */
    boolean canDeploy(UserWorkspaceProject project);

    /**
     * Check if project can be deleted
     *
     * @param project project
     * @return true or false
     */
    boolean canDelete(UserWorkspaceProject project);

    /**
     * Check if project can be erased
     *
     * @param project project
     * @return true or false
     */
    boolean canErase(UserWorkspaceProject project);
}
