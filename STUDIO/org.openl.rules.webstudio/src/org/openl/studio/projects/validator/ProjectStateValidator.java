package org.openl.studio.projects.validator;

import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.project.abstraction.UserWorkspaceProject;

/**
 * Project state validator
 */
public interface ProjectStateValidator {

    /**
     * Checks whether a modified project can be committed to its Design repository.
     *
     * <p>A project that exists only in the local workspace has no Design repository revision to save.
     * It must be imported instead.
     *
     * @param project project
     * @return {@code true} when the project can be saved
     */
    boolean canSave(UserWorkspaceProject project);

    /**
     * Check if project can be modified
     *
     * @param project project
     * @return true or false
     */
    boolean canModify(UserWorkspaceProject project);

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
     * Check if project can be merged
     *
     * @param project project
     * @return true or false
     */
    boolean canMerge(RulesProject project);

    /**
     * Check if the branch the project sits on can be deleted.
     *
     * <p>The repository base branch never can, and a protected branch needs the right to bypass branch
     * protection. Whether the deletion also removes the project is a separate question, answered by
     * the design-time repository.
     *
     * @param project project
     * @return true or false
     */
    boolean canDeleteBranch(RulesProject project);

}
