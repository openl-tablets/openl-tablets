package org.openl.rules.webstudio.web.servlet;

import java.util.Optional;

import lombok.Getter;
import lombok.Setter;

import org.openl.rules.repository.api.UserInfo;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.webstudio.service.UserManagementService;
import org.openl.rules.workspace.MultiUserWorkspaceManager;
import org.openl.rules.workspace.WorkspaceUserImpl;
import org.openl.rules.workspace.uw.UserWorkspace;

public class RulesUserSession {

    @Getter
    @Setter
    private String userName;

    private UserWorkspace userWorkspace;

    @Getter
    @Setter
    private WebStudio webStudio;

    @Setter
    private MultiUserWorkspaceManager workspaceManager;

    @Setter
    private UserManagementService userManagementService;

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
}
