package org.openl.studio.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.security.authentication.event.InteractiveAuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

import org.openl.rules.workspace.lw.LocalWorkspaceManager;

/**
 * Cleans up and recomputes the workspace metainfo registry of a user on every sign-in.
 *
 * <p>The registry reconciles garbage left in the user workspace (a project folder without a record,
 * a record without a folder, an unreadable record) and recomputes the local-changes state of the
 * opened projects.
 *
 * <p>The reconciliation walks project files on disk, so it runs in the background: the sign-in
 * latency does not depend on the workspace size, and a reconciliation failure cannot fail the
 * sign-in.
 *
 * <p>Reacts on interactive sign-ins only, such as the login form or an SSO redirect. Stateless
 * requests, like Basic authentication or personal access tokens, do not start a login session and
 * do not trigger the reconciliation.
 *
 * @author Yury Molchan
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WorkspaceRegistryReconciler implements ApplicationListener<InteractiveAuthenticationSuccessEvent> {

    private final LocalWorkspaceManager localWorkspaceManager;

    @Override
    public void onApplicationEvent(InteractiveAuthenticationSuccessEvent event) {
        String username = event.getAuthentication().getName();
        Thread.ofVirtual().name("workspace-reconcile-" + username).start(() -> reconcile(username));
    }

    private void reconcile(String username) {
        try {
            localWorkspaceManager.refreshMetainfoRegistry(username);
        } catch (Exception e) {
            log.error("Failed to reconcile the workspace metainfo registry for user '{}'.", username, e);
        }
    }
}
