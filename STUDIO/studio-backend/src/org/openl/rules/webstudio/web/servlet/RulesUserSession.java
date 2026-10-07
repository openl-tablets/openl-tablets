package org.openl.rules.webstudio.web.servlet;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import jakarta.annotation.PreDestroy;

import lombok.Getter;
import lombok.Setter;

import org.openl.rules.repository.api.UserInfo;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.webstudio.service.UserManagementService;
import org.openl.rules.workspace.MultiUserWorkspaceManager;
import org.openl.rules.workspace.WorkspaceUserImpl;
import org.openl.rules.workspace.uw.UserWorkspace;

/**
 * The state OpenL Studio keeps for one client of a user: a browser's HTTP session, or the credentials a
 * stateless request carries.
 *
 * Only the user name is serializable state. The workspace, the studio and the services behind them are transient:
 * the session is never persisted or replicated, and a session that has no holder gets a fresh one from the
 * application context.
 *
 * <p>The holder ends with the client: the workspace is handed back and the studio torn down. The workspace is
 * shared by every client of the user and is released only when the last of them ends.
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
            userWorkspace = Objects.requireNonNull(workspaceManager, "workspaceManager is not set")
                    .acquireUserWorkspace(getWorkspaceUser());
            userWorkspace.activate();
        }

        return userWorkspace;
    }

    private WorkspaceUserImpl getWorkspaceUser() {
        return new WorkspaceUserImpl(getUserName(),
                username -> Optional.ofNullable(userManagementService.getUser(username))
                        .map(usr -> new UserInfo(usr.getUsername(), usr.getEmail(), usr.getDisplayName()))
                        .orElse(null));
    }

    /**
     * Lets go of what the client held.
     *
     * <p>Called when the session is invalidated or expires, or the credentials of a stateless client have been
     * idle for as long. The workspace is handed back, then the studio is torn down.
     */
    @PreDestroy
    void sessionDestroyed() {
        if (userWorkspace != null) {
            workspaceManager.releaseUserWorkspace(userWorkspace);
        }
        if (webStudio != null) {
            webStudio.destroy();
        }
    }
}
