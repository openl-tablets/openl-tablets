package org.openl.rules.webstudio;

import org.openl.rules.MultiUserWorkspaceManager;
import org.openl.rules.WorkspaceException;
import org.openl.rules.WorkspaceUser;
import org.openl.rules.commons.logs.CLog;
import org.openl.rules.commons.projects.ProjectException;
import org.openl.rules.uw.UserWorkspace;

import javax.servlet.http.HttpSessionActivationListener;
import javax.servlet.http.HttpSessionEvent;
import java.io.File;

public class RulesUserSession implements HttpSessionActivationListener {
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

    public UserWorkspace getUserWorkspace() throws WorkspaceException {
        if (userWorkspace == null) {
            userWorkspace = workspaceManager.getUserWorkspace(user);
        }
        
        return userWorkspace;
    }

    public void sessionWillPassivate(HttpSessionEvent event) {
        userWorkspace.passivate();
    }

    public void sessionDidActivate(HttpSessionEvent event) {
        try {
            userWorkspace.activate();
        } catch (ProjectException e) {
            CLog.log(CLog.ERROR, "Error at activation", e);
        }
    }

    public void sessionDestroyed() {
        userWorkspace.release();
    }
}
