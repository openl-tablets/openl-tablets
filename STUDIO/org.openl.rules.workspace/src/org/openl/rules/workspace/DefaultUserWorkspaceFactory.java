package org.openl.rules.workspace;

import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.rules.workspace.lw.LocalWorkspace;
import org.openl.rules.workspace.lw.LocalWorkspaceManager;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.rules.workspace.uw.impl.UserWorkspaceImpl;

public class DefaultUserWorkspaceFactory implements UserWorkspaceFactory {
    @Override
    public UserWorkspace create(LocalWorkspaceManager localWorkspaceManager,
                                DesignTimeRepository designTimeRepository,
                                WorkspaceUser user) {
        LocalWorkspace userLocalWorkspace = localWorkspaceManager.getWorkspace(user.getUserId());
        return new UserWorkspaceImpl(user,
                userLocalWorkspace,
                designTimeRepository,
                localWorkspaceManager.getLockEngine("projects"),
                localWorkspaceManager.getLockEngine("deploy-configs"));
    }
}
