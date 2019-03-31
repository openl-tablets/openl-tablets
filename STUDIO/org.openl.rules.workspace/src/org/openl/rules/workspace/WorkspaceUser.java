package org.openl.rules.workspace;

import org.openl.rules.common.CommonUser;

/**
 * User or Owner of Workspace.
 *
 * @author Aleh Bykhavets
 *
 */
public interface WorkspaceUser extends CommonUser, Comparable<WorkspaceUser> {
    /**
     * Returns identifier of user.
     * <p/>
     * Identifier should be unique and do not contain system unsafe symbols.
     *
     * @return id of user
     */
    String getUserId();
}
