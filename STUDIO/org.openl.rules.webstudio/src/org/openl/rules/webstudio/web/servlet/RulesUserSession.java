package org.openl.rules.webstudio.web.servlet;

import java.io.Serial;
import java.io.Serializable;
import java.util.Optional;

import lombok.Getter;
import lombok.Setter;

import org.openl.rules.repository.api.UserInfo;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.webstudio.service.UserManagementService;
import org.openl.rules.workspace.MultiUserWorkspaceManager;
import org.openl.rules.workspace.WorkspaceUserImpl;
import org.openl.rules.workspace.uw.UserWorkspace;

/**
 * The state OpenL Studio keeps for a user in the HTTP session.
 *
 * Only the user name is serializable state. The workspace, the studio and the services behind them are transient:
 * the session is never persisted or replicated, and a session that has no holder gets a fresh one from the
 * application context.
 */
public class RulesUserSession implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Getter
    @Setter
    private String userName;

    private transient UserWorkspace userWorkspace;

    @Getter
    @Setter
    private transient WebStudio webStudio;

    @Setter
    private transient MultiUserWorkspaceManager workspaceManager;

    @Setter
    private transient UserManagementService userManagementService;

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
}
