package org.openl.rules.ruleservice.loader;

import org.openl.rules.common.CommonVersion;
import org.openl.rules.common.ProjectException;
import org.openl.rules.common.impl.ArtefactPathImpl;
import org.openl.rules.project.abstraction.Deployment;
import org.openl.rules.project.impl.local.LocalFolderAPI;
import org.openl.rules.ruleservice.core.RuleServiceRuntimeException;
import org.openl.rules.workspace.lw.impl.FolderHelper;
import org.openl.rules.workspace.lw.impl.LocalWorkspaceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileFilter;
import java.util.HashMap;
import java.util.Map;

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
        if (directoryToLoadDeploymentsIn == null)
            throw new IllegalArgumentException("directoryToLoadDeploymentsIn argument can't be null");
        this.directoryToLoadDeploymentsIn = directoryToLoadDeploymentsIn;
    }

    /**
     * Generates folder name for deployment by given deployment name and common
     * version.
     *
     * @return folder name
     */
    protected String getDeploymentFolderName(String deploymentName, CommonVersion version) {
        return String.format("%s_v%s", deploymentName, version.getVersionName());
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

    private File getDeploymentFolder(String deploymentName, CommonVersion version) {
        File deploymentFolder = new File(getFolderToLoadDeploymentsIn(), getDeploymentFolderName(deploymentName,
                version));
        return deploymentFolder;
    }

    /**
     * Gets deployment from storage. If deployment doesn't exists in storage
     * returns null.
     *
     * @return deployment from storage or null if doens't exists
     */
    public Deployment getDeployment(String deploymentName, CommonVersion version) {
        if (deploymentName == null) {
            throw new IllegalArgumentException("deploymentName argument can't be null");
        }
        if (version == null) {
            throw new IllegalArgumentException("version argument can't be null");
        }

        log.debug("Getting deployment with name=\"{}\" and version=\"{}\"", deploymentName, version.getVersionName());

        if (containsDeployment(deploymentName, version)) {
            Deployment deployment = cacheForGetDeployment.get(getDeploymentFolderName(deploymentName, version));
            if (deployment != null) {
                log.debug("Getting deployment with name=\"{}\" and version=\"{}\" has been returned from cache.", deploymentName, version.getVersionName());
                return deployment;
            }
            File deploymentFolder = getDeploymentFolder(deploymentName, version);
            LocalFolderAPI localFolderAPI = new LocalFolderAPI(deploymentFolder, new ArtefactPathImpl(
                    deploymentFolder.getName()), new LocalWorkspaceImpl(null, deploymentFolder.getParentFile(),
                    getLocalWorkspaceFolderFilter(), getLocalWorkspaceFileFilter()));
            deployment = new Deployment(localFolderAPI);
            cacheForGetDeployment.put(getDeploymentFolderName(deploymentName, version), deployment);
            log.debug("Deployment with name=\"{}\" and version=\"{}\" has been returned from local storage and putted to cache.", deploymentName, version.getVersionName());
            return deployment;
        } else {
            log.debug("Deployment with name=\"{}\" and version=\"{}\" hasn't been found in local storage.", deploymentName, version.getVersionName());
            return null;
        }
    }

    /**
     * Loads deployment to local file system from repository.
     *
     * @param deployment
     * @return loaded deployment
     */
    public Deployment loadDeployment(Deployment deployment) {
        if (deployment == null) {
            throw new IllegalArgumentException("deployment argument can't be null");
        }

        log.debug("Loading deployement with name=\"{}\" and version=\"{}\"", deployment.getDeploymentName(), deployment.getVersion().getVersionName());

        File deploymentFolder = getDeploymentFolder(deployment.getDeploymentName(), deployment.getCommonVersion());
        LocalFolderAPI localFolderAPI = new LocalFolderAPI(deploymentFolder, new ArtefactPathImpl(
                deploymentFolder.getName()), new LocalWorkspaceImpl(null, deploymentFolder.getParentFile(),
                getLocalWorkspaceFolderFilter(), getLocalWorkspaceFileFilter()));
        Deployment loadedDeployment = new Deployment(localFolderAPI);
        try {
            loadedDeployment.update(deployment, null);
            loadedDeployment.refresh();
        } catch (ProjectException e) {
            log.warn("Exception occurs on loading deployment with name=\"{}\" and version=\"{}\" from data source", deployment.getDeploymentName(), deployment.getVersion().getVersionName(), e);
            throw new RuleServiceRuntimeException(e);
        }

        cacheForGetDeployment.remove(getDeploymentFolderName(deployment.getDeploymentName(),
                deployment.getCommonVersion()));
        log.debug("Deployement with name=\"{}\" and version=\"{}\" has been removed from cache.", deployment.getDeploymentName(), deployment.getVersion().getVersionName());
        return loadedDeployment;
    }

    /**
     * Remove deployment to local file system from repository.
     *
     * @return true if and only if the file or directory is successfully
     * deleted; false otherwise
     */
    public boolean removeDeployment(String deploymentName, CommonVersion version) {
        if (deploymentName == null) {
            throw new IllegalArgumentException("deploymentName argument can't be null");
        }
        if (version == null) {
            throw new IllegalArgumentException("version argument can't be null");
        }

        log.debug("Removing deployement with name=\"{}\" and version=\"{}\"", deploymentName, version.getVersionName());

        cacheForGetDeployment.remove(getDeploymentFolderName(deploymentName, version));
        log.debug("Deployement with name=\"{}\" and version=\"{}\" has been removed from cache.", deploymentName, version.getVersionName());

        return FolderHelper.clearFolder(getDeploymentFolder(deploymentName, version));
    }

    /**
     * Check to existing deployment in local temporary folder.
     *
     * @return true if and only if the deployment exists; false otherwise
     */
    public boolean containsDeployment(String deploymentName, CommonVersion version) {
        if (deploymentName == null) {
            throw new IllegalArgumentException("deploymentName argument can't be null");
        }
        if (version == null) {
            throw new IllegalArgumentException("version argument can't be null");
        }
        return getDeploymentFolder(deploymentName, version).exists();
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
