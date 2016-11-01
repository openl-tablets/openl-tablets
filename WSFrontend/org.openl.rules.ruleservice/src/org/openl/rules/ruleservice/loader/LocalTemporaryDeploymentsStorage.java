package org.openl.rules.ruleservice.loader;

import java.io.File;
import java.io.FileFilter;
import java.util.HashMap;
import java.util.Map;

import org.openl.rules.common.CommonVersion;
import org.openl.rules.common.ProjectException;
import org.openl.rules.project.abstraction.Deployment;
import org.openl.rules.project.impl.local.LocalRepository;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.ruleservice.core.RuleServiceRuntimeException;
import org.openl.rules.workspace.lw.impl.FolderHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Local temporary file system storage for deployments. Clears all data on first
 * initialization. Thread safe implementation.
 *
 * @author Marat Kamalov
 */
public class LocalTemporaryDeploymentsStorage {

    private final Logger log = LoggerFactory.getLogger(LocalTemporaryDeploymentsStorage.class);

    private File folderToLoadDeploymentsIn;

    private String directoryToLoadDeploymentsIn;

    private final static Object flag = new Object();

    private FileFilter localWorkspaceFolderFilter;

    private FileFilter localWorkspaceFileFilter;

    private Map<String, Deployment> cacheForGetDeployment = new HashMap<String, Deployment>();

    /**
     * Construct a new LocalTemporaryDeploymentsStorage for bean usage.
     */
    public LocalTemporaryDeploymentsStorage() {
        directoryToLoadDeploymentsIn = System.getProperty("user.home") + "/.openl/tmp";
    }

    /**
     * Construct a new LocalTemporaryDeploymentsStorage for bean usage.
     *
     * @see #setLocalWorkspaceFileFilter, #setLocalWorkspaceFolderFilter
     */
    public LocalTemporaryDeploymentsStorage(FileFilter localWorkspaceFolderFilter, FileFilter localWorkspaceFileFilter) {
        this.localWorkspaceFolderFilter = localWorkspaceFolderFilter;
        this.localWorkspaceFileFilter = localWorkspaceFileFilter;
    }

    /**
     * Sets localWorkspaceFileFilter @see LocalFolderAPI. Spring bean
     * configuration property.
     *
     * @param localWorkspaceFileFilter
     */
    public void setLocalWorkspaceFileFilter(FileFilter localWorkspaceFileFilter) {
        this.localWorkspaceFileFilter = localWorkspaceFileFilter;
    }

    /**
     * Gets localWorkspaceFileFilter.
     */
    public FileFilter getLocalWorkspaceFileFilter() {
        return localWorkspaceFileFilter;
    }

    /**
     * Sets localWorkspaceFolderFilter @see LocalFolderAPI. Spring bean
     * configuration property.
     *
     * @param localWorkspaceFolderFilter
     */
    public void setLocalWorkspaceFolderFilter(FileFilter localWorkspaceFolderFilter) {
        this.localWorkspaceFolderFilter = localWorkspaceFolderFilter;
    }

    /**
     * Gets localWorkspaceFolderFilter.
     */
    public FileFilter getLocalWorkspaceFolderFilter() {
        return localWorkspaceFolderFilter;
    }

    /**
     * Gets path to folder to load deployments in directory. Extract the value
     * with key <i>ruleservice.deployment.storage.dir</i> from configuration
     * file. If such a key is missing returns default value
     * <tt>/tmp/rules-deploy</tt>.
     *
     * @return path to load in directory
     */
    public String getDirectoryToLoadDeploymentsIn() {
        return directoryToLoadDeploymentsIn;
    }

    /**
     * Sets a path to local temporary storage. Spring bean configuration
     * property.
     *
     * @param directoryToLoadDeploymentsIn
     */
    public void setDirectoryToLoadDeploymentsIn(String directoryToLoadDeploymentsIn) {
        if (directoryToLoadDeploymentsIn == null) {
            throw new IllegalArgumentException("directoryToLoadDeploymentsIn argument can't be null");
        }
        this.directoryToLoadDeploymentsIn = directoryToLoadDeploymentsIn;
    }

    /**
     * Generates folder name for deployment by given deployment name and common
     * version.
     *
     * @return folder name
     */
    private String getDeploymentFolderName(String deploymentName, CommonVersion version) {
        return new StringBuilder(deploymentName).append("_v").append(version.getVersionName()).toString();
    }

    private File getFolderToLoadDeploymentsIn() {
        if (folderToLoadDeploymentsIn == null) {
            synchronized (flag) {
                if (folderToLoadDeploymentsIn == null) {
                    folderToLoadDeploymentsIn = new File(getDirectoryToLoadDeploymentsIn());
                    folderToLoadDeploymentsIn.mkdirs();
                    clear();
                }
            }
            log.info("Local temporary folder location is: {}", getDirectoryToLoadDeploymentsIn());
        }
        return folderToLoadDeploymentsIn;
    }

    /**
     * Gets deployment from storage. If deployment doesn't exists in storage
     * returns null.
     *
     * @return deployment from storage or null if doens't exists
     */
    Deployment getDeployment(String deploymentName, CommonVersion version) {
        log.debug("Getting deployment with name=\"{}\" and version=\"{}\"", deploymentName, version.getVersionName());
        String deploymentFolderName = getDeploymentFolderName(deploymentName, version);
        Deployment deployment = cacheForGetDeployment.get(deploymentFolderName);
        return deployment;
    }

    /**
     * Loads deployment to local file system from repository.
     *
     * @param deployment
     * @return loaded deployment
     */
    Deployment loadDeployment(Deployment deployment) {
        if (deployment == null) {
            throw new IllegalArgumentException("deployment argument can't be null");
        }

        String deploymentName = deployment.getDeploymentName();
        CommonVersion version = deployment.getCommonVersion();
        String versionName = deployment.getVersion().getVersionName();
        log.debug("Loading deployement with name=\"{}\" and version=\"{}\"", deploymentName, versionName);

        String deploymentFolderName = getDeploymentFolderName(deploymentName, version);
        Repository repository = new LocalRepository(getFolderToLoadDeploymentsIn());
        Deployment loadedDeployment = new Deployment(repository, deploymentFolderName, deploymentName, version);
        try {
            loadedDeployment.update(deployment, null);
            loadedDeployment.refresh();
        } catch (ProjectException e) {
            log.warn("Exception occurs on loading deployment with name=\"{}\" and version=\"{}\" from data source",
                deploymentName,
                versionName,
                e);
            throw new RuleServiceRuntimeException(e);
        }

        cacheForGetDeployment.put(deploymentFolderName, loadedDeployment);

        log.debug("Deployment with name=\"{}\" and version=\"{}\" has been made on local storage and putted to cache.",
            deploymentName,
            versionName);
        return loadedDeployment;
    }

    /**
     * Check to existing deployment in local temporary folder.
     *
     * @return true if and only if the deployment exists; false otherwise
     */
    boolean containsDeployment(String deploymentName, CommonVersion version) {
        return cacheForGetDeployment.containsKey(getDeploymentFolderName(deploymentName, version));
    }

    /**
     * Clear storage.
     */
    public void clear() {
        synchronized (flag) {
            cacheForGetDeployment.clear();
            File folder = getFolderToLoadDeploymentsIn();
            if (!FolderHelper.clearFolder(folder)) {
                log.error("Failed to clear a folder \"{}\"!", folder.getAbsolutePath());
            } else {
                log.info("Local temporary folder for downloading deployments was cleared.");
            }
        }
    }
}
