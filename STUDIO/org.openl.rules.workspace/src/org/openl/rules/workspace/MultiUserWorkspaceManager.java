package org.openl.rules.workspace;

import org.openl.rules.workspace.deploy.DeploymentException;
import org.openl.rules.workspace.deploy.ProductionDeployerManager;
import org.openl.rules.workspace.deploy.ProductionDeployer;
import org.openl.rules.workspace.deploy.impl.jcr.JcrProductionDeployer;
import org.openl.rules.workspace.deploy.impl.ProductionDeployerManagerImpl;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.rules.workspace.dtr.RepositoryException;
import org.openl.rules.workspace.dtr.impl.DesignTimeRepositoryImpl;
import org.openl.rules.workspace.lw.LocalWorkspace;
import org.openl.rules.workspace.lw.LocalWorkspaceManager;
import org.openl.rules.workspace.lw.impl.LocalWorkspaceManagerImpl;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.rules.workspace.uw.UserWorkspaceListener;
import org.openl.rules.workspace.uw.impl.UserWorkspaceImpl;

import java.util.HashMap;
import java.util.Map;

public class MultiUserWorkspaceManager implements UserWorkspaceListener{
    private ProductionDeployerManager deployerManager;
    private LocalWorkspaceManager localManager;
    private DesignTimeRepository designTimeRepository;
    private Map<String, UserWorkspace> userWorkspaces;

    public MultiUserWorkspaceManager() throws WorkspaceException {
        userWorkspaces = new HashMap<String, UserWorkspace>();

        localManager = new LocalWorkspaceManagerImpl();
        deployerManager = new ProductionDeployerManagerImpl();
        try {
            designTimeRepository = new DesignTimeRepositoryImpl();
        } catch (RepositoryException e) {
            throw new WorkspaceException("Cannot init Design Time Repository", e);
        }        
    }

    public UserWorkspace getUserWorkspace(WorkspaceUser user) throws WorkspaceException, DeploymentException {
        UserWorkspace uw = userWorkspaces.get(user.getUserId());
        if (uw == null) {
            uw = createUserWorkspace(user);
            userWorkspaces.put(user.getUserId(), uw);
        }

        return uw;
    }

    protected UserWorkspace createUserWorkspace(WorkspaceUser user) throws WorkspaceException {
        LocalWorkspace lw = localManager.getWorkspace(user);
        ProductionDeployer deployer;
        try {
            deployer = deployerManager.getDeployer(user);
        } catch (DeploymentException e) {
            throw new WorkspaceException("can not get production deployer", e);
        }
        return new UserWorkspaceImpl(user, lw, designTimeRepository, deployer);
    }

    public void workspaceReleased(UserWorkspace workspace) {
        userWorkspaces.remove(((UserWorkspaceImpl) workspace).getUser().getUserId());
    }
}
