package org.openl.rules.workspace.production.client;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.repository.ProductionRepositoryFactoryProxy;
import org.openl.rules.repository.RDeploymentListener;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.Listener;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.repository.exceptions.RRepositoryException;
import org.openl.rules.repository.file.FileRepository;
import org.openl.rules.workspace.deploy.DeployID;
import org.openl.rules.workspace.deploy.DeployUtils;
import org.openl.rules.workspace.lw.impl.FolderHelper;

/**
 * This class can extract rules projects deployed into production JCR based
 * environment to a specified location on file system. Also it is a good place
 * for higher level utility methods on top of production repository API.
 */
public class JcrRulesClient {
    private ProductionRepositoryFactoryProxy productionRepositoryFactoryProxy;
    private String repositoryPropertiesFile;
    private final List<RDeploymentListener> listeners = new ArrayList<RDeploymentListener>();
    private final DeploymentListenersListenersWrapper listenersWrapper = new DeploymentListenersListenersWrapper(listeners);

    public JcrRulesClient(ProductionRepositoryFactoryProxy productionRepositoryFactoryProxy,
            String repositoryPropertiesFile) {
        this.productionRepositoryFactoryProxy = productionRepositoryFactoryProxy;
        this.repositoryPropertiesFile = repositoryPropertiesFile;
    }

    public void addListener(RDeploymentListener l) throws RRepositoryException {
        listeners.add(l);
        productionRepositoryFactoryProxy.getRepositoryInstance(repositoryPropertiesFile).setListener(listenersWrapper);
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
        Repository localRepository = new FileRepository(destFolder.getParentFile());
        AProject fetchedDeployment = new AProject(localRepository, destFolder.getName());

        Repository productionRepository = productionRepositoryFactoryProxy.getRepositoryInstance(repositoryPropertiesFile);
        final AProject deploymentProject = new AProject(productionRepository, DeployUtils.DEPLOY_PATH + deployID.getName());
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

        fetchedDeployment.update(deploymentProject, null);
    }

    /**
     * Returns names of all existing deployments in production repository.
     *
     * @return collection of names
     * @throws RRepositoryException on repository error
     */
    public Collection<String> getDeploymentNames() throws RRepositoryException {
        Collection<FileData> fileDatas = null;
        try {
            fileDatas = productionRepositoryFactoryProxy.getRepositoryInstance(repositoryPropertiesFile) .list(DeployUtils.DEPLOY_PATH);
        } catch (IOException e) {
            throw new RRepositoryException("Cannot read the deploy repository", e);
        }
        Collection<String> result = new ArrayList<String>();
        for (FileData fileData : fileDatas) {
            result.add(fileData.getName());
        }
        return result;
    }

    public void release() throws RRepositoryException {
        productionRepositoryFactoryProxy.destroy();
    }

    public void removeListener(RDeploymentListener l) throws RRepositoryException {
        listeners.remove(l);
        if (listeners.isEmpty()) {
            productionRepositoryFactoryProxy.getRepositoryInstance(repositoryPropertiesFile).setListener(null);
        }
    }

    private static class DeploymentListenersListenersWrapper implements Listener {
        private final List<RDeploymentListener> listeners;

        private DeploymentListenersListenersWrapper(List<RDeploymentListener> listeners) {
            this.listeners = listeners;
        }

        @Override
        public void onChange() {
            for (RDeploymentListener listener : listeners) {
                listener.onEvent();
            }
        }
    }
}
