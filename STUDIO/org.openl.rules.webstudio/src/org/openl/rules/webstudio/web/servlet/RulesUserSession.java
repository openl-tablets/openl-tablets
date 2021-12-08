package org.openl.rules.webstudio.web.servlet;

import java.util.Optional;

import org.openl.rules.repository.api.UserInfo;
import org.openl.rules.webstudio.service.UserManagementService;
import org.openl.rules.workspace.MultiUserWorkspaceManager;
import org.openl.rules.workspace.WorkspaceUserImpl;
import org.openl.rules.workspace.uw.UserWorkspace;

public class RulesUserSession {

    private String userName;

    private UserWorkspace userWorkspace;

    private MultiUserWorkspaceManager workspaceManager;

    private UserManagementService userManagementService;

    public String getUserName() {
        return userName;
    }

    public synchronized UserWorkspace getUserWorkspace() {
        if (userWorkspace == null) {
            userWorkspace = workspaceManager.getUserWorkspace(getWorkspaceUser());
            userWorkspace.activate();
        }

        return userWorkspace;
    }

    private WorkspaceUserImpl getWorkspaceUser() {
        return new WorkspaceUserImpl(getUserName(),
            (username) -> Optional.ofNullable(userManagementService.getUser(username))
                .map(usr -> new UserInfo(usr.getUsername(), usr.getEmail(), usr.getDisplayName()))
                .orElse(null));
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

    public void setUserManagementService(UserManagementService userManagementService) {
        this.userManagementService = userManagementService;
    }
}
