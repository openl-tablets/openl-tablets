package org.openl.rules.webstudio.web.servlet;

import org.openl.rules.workspace.MultiUserWorkspaceManager;
import org.openl.rules.workspace.WorkspaceException;
import org.openl.rules.workspace.WorkspaceUserImpl;
import org.openl.rules.workspace.uw.UserWorkspace;

public class RulesUserSession {

    private String userName;

    private UserWorkspace userWorkspace;

    private MultiUserWorkspaceManager workspaceManager;

    public String getUserName() {
        return userName;
    }

    public synchronized UserWorkspace getUserWorkspace() throws WorkspaceException {
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
        userWorkspace.activate();
    }

    public void sessionWillPassivate() {
        userWorkspace.passivate();
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public void setWorkspaceManager(MultiUserWorkspaceManager workspaceManager) {
        this.workspaceManager = workspaceManager;
    }
}
