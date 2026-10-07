package org.openl.studio.projects.validator;

/**
 * Why the current user cannot modify a project now.
 *
 * @author Yury Molchan
 */
public enum ModifyRestriction {

    /**
     * The branch of the project is protected, and the current user cannot bypass the protection.
     */
    BRANCH_PROTECTED,

    /**
     * The project is locked, and the current user does not have it opened for editing: another user is editing it,
     * or a lock of the current user outlived the editing.
     */
    LOCKED
}
