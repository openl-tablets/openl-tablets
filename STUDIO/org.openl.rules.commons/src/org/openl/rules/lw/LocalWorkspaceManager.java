package org.openl.rules.lw;

import org.openl.rules.WorkspaceUser;
import org.openl.rules.WorkspaceException;

public interface LocalWorkspaceManager {
    LocalWorkspace getWorkspace(WorkspaceUser user) throws WorkspaceException;
}
