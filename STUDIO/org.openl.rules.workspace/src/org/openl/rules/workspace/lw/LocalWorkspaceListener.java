package org.openl.rules.workspace.lw;

import java.util.EventListener;

public interface LocalWorkspaceListener extends EventListener {
    void workspaceReleased(LocalWorkspace workspace);
}
