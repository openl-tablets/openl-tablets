package org.openl.rules.workspace;

import java.util.HashMap;
import java.util.Map;

import org.openl.rules.workspace.deploy.DeploymentException;
import org.openl.rules.workspace.deploy.ProductionDeployer;
import org.openl.rules.workspace.deploy.ProductionDeployerManager;
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

/**
 * Manager of Multiple User Workspaces.
 * <p/>
 * It takes care of creation and releasing of User Workspaces.
 * Also, it initializes Local Workspace Manager and Design Time Repository.
 *
 * @author Aleh Bykhavets
 *
 */
public class MultiUserWorkspaceManager implements UserWorkspaceListener{
    private ProductionDeployerManager deployerManager;
    /** Design Time Repository */
    private DesignTimeRepository designTimeRepository;
    /** Cache for User Workspaces */
    private Map<String, UserWorkspace> userWorkspaces;
    /** Manager of Local Workspaces */
    private LocalWorkspaceManager localWorkspaceManager;

    public void setLocalWorkspaceManager(LocalWorkspaceManager localWorkSpaceManager) {
        this.localWorkspaceManager = localWorkSpaceManager;
    }

    public MultiUserWorkspaceManager() throws WorkspaceException {
        userWorkspaces = new HashMap<String, UserWorkspace>();
        deployerManager = new ProductionDeployerManagerImpl();
        try {
            designTimeRepository = new DesignTimeRepositoryImpl();
        } catch (RepositoryException e) {
            throw new WorkspaceException("Cannot init Design Time Repository", e);
        }
    }

    /**
     * Returns .
     * <p/>
     * It creates Workspace (including local) for specified user
     * on first request.
     *
     * @param user active user
     * @return new or cached instance of user workspace
     *
     * @throws WorkspaceException if failed
     */
    public UserWorkspace getUserWorkspace(WorkspaceUser user) throws WorkspaceException {
        UserWorkspace uw = userWorkspaces.get(user.getUserId());
        if (uw == null) {
            uw = createUserWorkspace(user);
            userWorkspaces.put(user.getUserId(), uw);
        }

        return uw;
    }

    protected UserWorkspace createUserWorkspace(WorkspaceUser user) throws WorkspaceException {
        LocalWorkspace usersLocalWorkspace = localWorkspaceManager.getWorkspace(user);
        ProductionDeployer deployer;
        try {
            deployer = deployerManager.getDeployer(user);
        } catch (DeploymentException e) {
            throw new WorkspaceException("can not get production deployer", e);
        }
        return new UserWorkspaceImpl(user, usersLocalWorkspace, designTimeRepository, deployer);
    }

    /**
     * UserWorkspace should notify manager that life cycle of
     * the workspace is ended and it must be removed from cache.
     */
    public void workspaceReleased(UserWorkspace workspace) {
        userWorkspaces.remove(((UserWorkspaceImpl) workspace).getUser().getUserId());
    }
}
