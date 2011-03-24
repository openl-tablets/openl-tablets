package org.openl.rules.workspace.production.client;

import java.io.File;
import java.util.Collection;

import org.openl.rules.common.impl.ArtefactPathImpl;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.impl.local.LocalFolderAPI;
import org.openl.rules.repository.ProductionRepositoryFactoryProxy;
import org.openl.rules.repository.RDeploymentListener;
import org.openl.rules.repository.api.FolderAPI;
import org.openl.rules.repository.exceptions.RRepositoryException;
import org.openl.rules.workspace.deploy.DeployID;
import org.openl.rules.workspace.lw.impl.FolderHelper;
import org.openl.rules.workspace.lw.impl.LocalWorkspaceImpl;

/**
 * This class can extract rules projects deployed into production JCR based
 * environment to a specified location on file system. Also it is a good place
 * for higher level utility methods on top of production repository API.
 */
public class JcrRulesClient {
    public void addListener(RDeploymentListener l) throws RRepositoryException {
        ProductionRepositoryFactoryProxy.getRepositoryInstance().addListener(l);
    }

    /**
     * Unpacks deployed project with given deploy id to <code>destFolder</code>.
     * The methods uses <code>RRepository</code> instance provided by
     * <code>ProductionRepositoryFactoryProxy</code> factory as production
     * repository.
     *
     * @param deployID identifier of deployed project
     * @param destFolder the folder to unpack the project to.
     *
     * @throws Exception if an error occurres
     */
    public void fetchDeployment(DeployID deployID, File destFolder) throws Exception {
        destFolder.mkdirs();
        FolderHelper.clearFolder(destFolder);
        //FIXME: avoid creating LocalWorkspace
        AProject fetchedDeployment = new AProject(new LocalFolderAPI(destFolder, new ArtefactPathImpl(
                destFolder.getName()), new LocalWorkspaceImpl(null, destFolder.getParentFile(), null, null)));

        FolderAPI rDeployment = ProductionRepositoryFactoryProxy.getRepositoryInstance().getDeploymentProject(
                deployID.getName());
        final AProject deploymentProject = new AProject(rDeployment);
        // TODO: solve problem with fetching deployment when it is not uploaded
        // completely
        if (deploymentProject.isLocked()) {
            //waiting when our deployment will be unlocked.
            Thread waiting = new Thread(new Runnable() {
                public void run() {
                    try {
                        while (deploymentProject.isLocked()) {
                        }
                    } catch (Exception e) {
                    }
                }
            });
            waiting.start();
            waiting.join();
        }

        fetchedDeployment.update(deploymentProject, null, 0, 0);
    }

    /**
     * Returns names of all existing deployments in production repository.
     *
     * @return collection of names
     * @throws RRepositoryException on repository error
     */
    public Collection<String> getDeploymentNames() throws RRepositoryException {
        return ProductionRepositoryFactoryProxy.getRepositoryInstance().getDeploymentProjectNames();
    }

    public void release() throws RRepositoryException {
        ProductionRepositoryFactoryProxy.release();
    }

    public void removeListener(RDeploymentListener l) throws RRepositoryException {
        ProductionRepositoryFactoryProxy.getRepositoryInstance().removeListener(l);
    }

}
