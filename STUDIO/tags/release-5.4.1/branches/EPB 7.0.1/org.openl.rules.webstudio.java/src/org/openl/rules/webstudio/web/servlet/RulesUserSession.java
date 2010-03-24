package org.openl.rules.webstudio.web.servlet;

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

    public void setUser(WorkspaceUser user) {
        this.user = user;
    }

    public void setWorkspaceManager(MultiUserWorkspaceManager workspaceManager) {
        this.workspaceManager = workspaceManager;
    }

    public MultiUserWorkspaceManager getWorkspaceManager() {
        return workspaceManager;
    }

    public synchronized UserWorkspace getUserWorkspace() throws WorkspaceException, ProjectException {
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
        if (userWorkspace != null) {
            userWorkspace.release();
        }
    }
}
