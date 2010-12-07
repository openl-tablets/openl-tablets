package org.openl.rules.webstudio.web.servlet;

import org.openl.rules.workspace.MultiUserWorkspaceManager;
import org.openl.rules.workspace.WorkspaceException;
import org.openl.rules.workspace.WorkspaceUserImpl;
import org.openl.rules.workspace.abstracts.ProjectException;
import org.openl.rules.workspace.deploy.DeploymentException;
import org.openl.rules.workspace.deploy.ProductionDeployer;
import org.openl.rules.workspace.deploy.ProductionDeployerManager;
import org.openl.rules.workspace.deploy.impl.ProductionDeployerManagerImpl;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.util.Log;

import org.springframework.security.core.userdetails.UserDetails;

public class RulesUserSession {

    private UserDetails user;
    private UserWorkspace userWorkspace;
    private MultiUserWorkspaceManager workspaceManager;
    private ProductionDeployer deployer;
    private ProductionDeployerManager deployerManager = new ProductionDeployerManagerImpl();

    public synchronized ProductionDeployer getDeployer() throws DeploymentException {
        if (deployer == null) {
            deployer = deployerManager.getDeployer(new WorkspaceUserImpl(user.getUsername()));
        }
        return deployer;
    }

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

    public MultiUserWorkspaceManager getWorkspaceManager() {
        return workspaceManager;
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
