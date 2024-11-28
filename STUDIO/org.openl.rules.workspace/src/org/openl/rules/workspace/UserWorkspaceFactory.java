package org.openl.rules.workspace;

import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.rules.workspace.lw.LocalWorkspaceManager;
import org.openl.rules.workspace.uw.UserWorkspace;

public interface UserWorkspaceFactory {
    UserWorkspace create(LocalWorkspaceManager localWorkspaceManager,
                         DesignTimeRepository designTimeRepository,
                         WorkspaceUser user);
}
