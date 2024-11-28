package org.openl.rules.workspace.lw;

import org.openl.rules.project.abstraction.LockEngine;

public interface LocalWorkspaceManager {
    LocalWorkspace getWorkspace(String userId);

    /**
     * @param type projects type, used as a subfolder name. For example "rules" or "deployments"
     */
    LockEngine getLockEngine(String type);
}
