package org.openl.rules.workspace;

import org.openl.rules.repository.CommonUser;

public interface WorkspaceUser extends CommonUser, Comparable<WorkspaceUser> {
    String getUserId();
}
