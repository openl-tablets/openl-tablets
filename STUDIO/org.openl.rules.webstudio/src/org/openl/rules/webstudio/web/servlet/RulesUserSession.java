package org.openl.rules.webstudio.web.servlet;

import org.openl.rules.common.ProjectException;
import org.openl.rules.workspace.MultiUserWorkspaceManager;
import org.openl.rules.workspace.WorkspaceException;
import org.openl.rules.workspace.WorkspaceUserImpl;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.util.Log;
import org.springframework.security.core.userdetails.UserDetails;

public class RulesUserSession {

    private UserDetails user;

    private UserWorkspace userWorkspace;

    private MultiUserWorkspaceManager workspaceManager;

    public String getUserName() {
        if (user == null) {
            return null;
        }
        return user.getUsername();
    }

    public synchronized UserWorkspace getUserWorkspace() throws WorkspaceException, ProjectException {
        if (userWorkspace == null) {
            userWorkspace = workspaceManager.getUserWorkspace(new WorkspaceUserImpl(getUserName()));
            userWorkspace.activate();
        }

        return userWorkspace;
    }

    public void sessionDestroyed() {
        if (userWorkspace != null) {
            userWorkspace.release();
        }
    }

    public void sessionDidActivate() {
        try {
            userWorkspace.activate();
        } catch (ProjectException e) {
            Log.error("Error at activation", e);
        }
    }

    public void sessionWillPassivate() {
        userWorkspace.passivate();
    }

    public void setUser(UserDetails user) {
        this.user = user;
    }

    public void setWorkspaceManager(MultiUserWorkspaceManager workspaceManager) {
        this.workspaceManager = workspaceManager;
    }
}
