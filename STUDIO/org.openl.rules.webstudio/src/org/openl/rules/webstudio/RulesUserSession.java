package org.openl.rules.webstudio;

import org.openl.rules.workspace.MultiUserWorkspaceManager;
import org.openl.rules.workspace.WorkspaceException;
import org.openl.rules.workspace.WorkspaceUser;
import org.openl.rules.workspace.abstracts.ProjectException;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.util.Log;

public class RulesUserSession {
    private WorkspaceUser user;
    private UserWorkspace userWorkspace;
    private MultiUserWorkspaceManager workspaceManager;

    public RulesUserSession(WorkspaceUser user, MultiUserWorkspaceManager workspaceManager) {
        this.user = user;
        this.workspaceManager = workspaceManager;
    }

    public String getUserId() {
        return user.getUserId();
    }

    public UserWorkspace getUserWorkspace() throws WorkspaceException, ProjectException {
        if (userWorkspace == null) {
            userWorkspace = workspaceManager.getUserWorkspace(user);
            userWorkspace.activate();
        }
        
        return userWorkspace;
    }

    public void sessionWillPassivate() {
        userWorkspace.passivate();
    }

    public void sessionDidActivate() {
        try {
            userWorkspace.activate();
        } catch (ProjectException e) {
            Log.error("Error at activation", e);
        }
    }

    public void sessionDestroyed() {
        userWorkspace.release();
    }
}
