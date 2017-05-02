package org.openl.rules.workspace.lw;

import org.openl.rules.project.impl.local.LockEngine;
import org.openl.rules.workspace.WorkspaceException;
import org.openl.rules.workspace.WorkspaceUser;

public interface LocalWorkspaceManager {
    LocalWorkspace getWorkspace(WorkspaceUser user) throws WorkspaceException;

    /**
     * @param type projects type, used as a subfolder name. For example "rules" or "deployments"
     */
    LockEngine getLockEngine(String type);
}
