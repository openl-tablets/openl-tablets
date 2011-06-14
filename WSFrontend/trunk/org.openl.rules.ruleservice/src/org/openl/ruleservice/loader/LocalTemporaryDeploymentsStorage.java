package org.openl.ruleservice.loader;

import java.io.File;
import java.io.FileFilter;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.rules.common.CommonVersion;
import org.openl.rules.common.ProjectException;
import org.openl.rules.common.impl.ArtefactPathImpl;
import org.openl.rules.project.abstraction.Deployment;
import org.openl.rules.project.impl.local.LocalFolderAPI;
import org.openl.rules.workspace.lw.impl.FolderHelper;
import org.openl.rules.workspace.lw.impl.LocalWorkspaceImpl;

/**
 * Local temporary file system storage for deployments. Clears all data on first
 * initialization.
 * 
 * @author MKamalov
 * 
 */
public class LocalTemporaryDeploymentsStorage {

    private static Log log = LogFactory.getLog(LocalTemporaryDeploymentsStorage.class);

    private static String DEPLOYMENTS_TMP_DIRECTORY_DEFAULT = "/tmp/rules-deploy";

    private File folderToLoadDeploymentsIn;

    private String directoryToLoadDeploymentsIn = DEPLOYMENTS_TMP_DIRECTORY_DEFAULT;

    private static Object flag = new Object();

    private FileFilter localWorkspaceFolderFilter;

    private FileFilter localWorkspaceFileFilter;

    private Map<String, Deployment> cacheForGetDeployment = new HashMap<String, Deployment>();
    
    /**
     * Construct a new LocalTemporaryDeploymentsStorage for bean usage.
     */
    public LocalTemporaryDeploymentsStorage() {
    }

    /**
     * Construct a new LocalTemporaryDeploymentsStorage for bean usage. 
     * @see #setLocalWorkspaceFileFilter, #setLocalWorkspaceFolderFilter 
     */
    public LocalTemporaryDeploymentsStorage(FileFilter localWorkspaceFolderFilter, FileFilter localWorkspaceFileFilter) {
        this.localWorkspaceFolderFilter = localWorkspaceFolderFilter;
        this.localWorkspaceFileFilter = localWorkspaceFileFilter;
    }
    
    /**
     * Sets localWorkspaceFileFilter @see LocalFolderAPI. Spring bean configuration property
     * @param localWorkspaceFileFilter
     */
    public void setLocalWorkspaceFileFilter(FileFilter localWorkspaceFileFilter) {
        this.localWorkspaceFileFilter = localWorkspaceFileFilter;
    }
    
    /**
     * Gets localWorkspaceFileFilter
     */
    public FileFilter getLocalWorkspaceFileFilter() {
        return localWorkspaceFileFilter;
    }
    
    /**
     * Sets localWorkspaceFolderFilter @see LocalFolderAPI. Spring bean configuration property
     * @param localWorkspaceFolderFilter
     */
    public void setLocalWorkspaceFolderFilter(FileFilter localWorkspaceFolderFilter) {
        this.localWorkspaceFolderFilter = localWorkspaceFolderFilter;
    }

    /**
     * Gets localWorkspaceFolderFilter
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
     * Sets a path to local temporary storage. Spring bean configuration property
     * @param directoryToLoadDeploymentsIn
     */
    public void setDirectoryToLoadDeploymentsIn(String directoryToLoadDeploymentsIn) {
        this.directoryToLoadDeploymentsIn = directoryToLoadDeploymentsIn;
    }

    /**
     * Generates folder name for deployment by given deployment name and common verison
     * 
     * @param deployment
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
                    boolean clearBeforeFirstUse = true;
                    if (clearBeforeFirstUse) {
                        clear();
                    }
                }
                log.info("Local temprorary folder for downloading deployments was initializated.");
            }
        }
        return folderToLoadDeploymentsIn;
    }

    private File getDeploymentFolder(String deploymentName, CommonVersion version) {
        File deploymentFolder = new File(getFolderToLoadDeploymentsIn(), getDeploymentFolderName(deploymentName,
                version));
        return deploymentFolder;
    }

    /**
     * Gets deployment from storage. If deployment doesn't exists in storage returns null.
     * 
     * @param deployment
     * @return deployment from storage or null if doens't exists
     */
    public Deployment getDeployment(String deploymentName, CommonVersion version) {
        if (deploymentName == null || version == null)
            throw new IllegalArgumentException();
        if (containsDeployment(deploymentName, version)) {
            Deployment deployment = cacheForGetDeployment.get(getDeploymentFolderName(deploymentName, version));
            if (deployment != null) {
                return deployment;
            }
            File deploymentFolder = getDeploymentFolder(deploymentName, version);
            LocalFolderAPI localFolderAPI = new LocalFolderAPI(deploymentFolder, new ArtefactPathImpl(
                    deploymentFolder.getName()), new LocalWorkspaceImpl(null, deploymentFolder.getParentFile(),
                    getLocalWorkspaceFolderFilter(), getLocalWorkspaceFileFilter()));
            deployment = new Deployment(localFolderAPI);
            cacheForGetDeployment.put(getDeploymentFolderName(deploymentName, version), deployment);
            return deployment;
        } else {
            return null;
        }
    }

    /**
     * Loads deployment to local file system from repository
     * 
     * @param deployment
     * @return loaded deployment
     */
    public Deployment loadDeployment(Deployment deployment) {
        if (deployment == null)
            throw new IllegalArgumentException();
        File deploymentFolder = getDeploymentFolder(deployment.getDeploymentName(), deployment.getCommonVersion());
        LocalFolderAPI localFolderAPI = new LocalFolderAPI(deploymentFolder, new ArtefactPathImpl(
                deploymentFolder.getName()), new LocalWorkspaceImpl(null, deploymentFolder.getParentFile(),
                getLocalWorkspaceFolderFilter(), getLocalWorkspaceFileFilter()));
        Deployment loadedDeployment = new Deployment(localFolderAPI);
        try {
            loadedDeployment.update(deployment, null, deployment.getCommonVersion().getMajor(), deployment
                    .getCommonVersion().getMinor());
            loadedDeployment.refresh();
        } catch (ProjectException e) {
            // FIXME
            throw new RuntimeException(e);
        }

        cacheForGetDeployment.remove(getDeploymentFolderName(deployment.getDeploymentName(),
                deployment.getCommonVersion()));
        return loadedDeployment;
    }

    /**
     * Remove deployment to local file system from repository
     * 
     * @param deployment
     * @return true if and only if the file or directory is successfully
     *         deleted; false otherwise
     */
    public boolean removeDeployment(String deploymentName, CommonVersion version) {
        if (deploymentName == null || version == null)
            throw new IllegalArgumentException();
        cacheForGetDeployment.remove(getDeploymentFolderName(deploymentName, version));
        return FolderHelper.clearFolder(getDeploymentFolder(deploymentName, version));
    }

    /**
     * Check to existing deployment in local temporary folder
     * 
     * @param deployment
     * @return true if and only if the deployment exists; false otherwise
     */
    public boolean containsDeployment(String deploymentName, CommonVersion version) {
        return getDeploymentFolder(deploymentName, version).exists();
    }

    /**
     * Clear storage
     */
    public void clear() {
        synchronized (flag) {
            cacheForGetDeployment.clear();
            FolderHelper.clearFolder(getFolderToLoadDeploymentsIn());
        }
    }

}
