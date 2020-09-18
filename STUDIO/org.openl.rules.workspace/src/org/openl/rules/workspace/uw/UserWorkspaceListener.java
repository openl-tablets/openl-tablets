package org.openl.rules.workspace.uw;

import java.util.EventListener;

public interface UserWorkspaceListener extends EventListener {
    void workspaceReleased(UserWorkspace workspace);

    default void workspaceRefreshed() {
    }
}
