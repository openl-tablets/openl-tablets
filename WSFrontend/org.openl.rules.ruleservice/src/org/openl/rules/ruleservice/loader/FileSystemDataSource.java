package org.openl.rules.ruleservice.loader;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.openl.rules.common.CommonVersion;
import org.openl.rules.common.impl.ArtefactPathImpl;
import org.openl.rules.common.impl.CommonVersionImpl;
import org.openl.rules.project.abstraction.Deployment;
import org.openl.rules.project.impl.local.LocalFolderAPI;
import org.openl.rules.workspace.lw.impl.LocalWorkspaceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

/**
 * File based data source. Thread safe implementation.
 *
 * @author Marat Kamalov
 */
public class FileSystemDataSource implements DataSource, InitializingBean {

    private final Logger log = LoggerFactory.getLogger(FileSystemDataSource.class);

    private static final CommonVersion FILESYSTEM_COMMON_VERSION = new CommonVersionImpl(0, 0, 1);

    private String loadDeploymentsFromDirectory;

    private File loadDeploymentsFromFolder;

    private FileFilter localWorkspaceFolderFilter;

    private FileFilter localWorkspaceFileFilter;

    private Object flag = new Object();

    List<DataSourceListener> listeners = new ArrayList<DataSourceListener>();

    private boolean supportDeployments = false;

    private boolean supportVersion = false;

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

    public FileSystemDataSource() {
    }

    public FileSystemDataSource(String loadDeploymentsFromDirectory) {
        if (loadDeploymentsFromDirectory == null) {
            throw new IllegalArgumentException("loadDeploymentsFromDirectory argument can't be null");
        }
        this.loadDeploymentsFromDirectory = loadDeploymentsFromDirectory;
    }

    public void setLoadDeploymentsFromDirectory(String loadDeploymentsFromDirectory) {
        this.loadDeploymentsFromDirectory = loadDeploymentsFromDirectory;
    }

    public String getLoadDeploymentsFromDirectory() {
        return loadDeploymentsFromDirectory;
    }

    private File getLoadDeploymentsFromFolder() {
        if (loadDeploymentsFromFolder == null) {
            synchronized (flag) {
                if (loadDeploymentsFromFolder == null) {
                    loadDeploymentsFromFolder = new File(getLoadDeploymentsFromDirectory());
                    if (!loadDeploymentsFromFolder.exists()) {
                        if (!loadDeploymentsFromFolder.mkdirs()) {
                            log.warn("File system data source folder \"{}\" creation was fail!",
                                getLoadDeploymentsFromDirectory());
                        } else {
                            log.info("File system data source \"{}\" was successfully created!",
                                getLoadDeploymentsFromDirectory());
                        }
                    }
                }
            }
        }
        return loadDeploymentsFromFolder;
    }

    private void validateFileSystemDataSourceFolder(File fileSystemDataSourceFolder) {
        if (!fileSystemDataSourceFolder.exists() || !fileSystemDataSourceFolder.isDirectory()) {
            throw new DataSourceException("File system data source folder \"" + getLoadDeploymentsFromDirectory() + "\"  doesn't exist");
        }
    }

    /**
     * {@inheritDoc}
     */
    public Deployment getDeployment(String deploymentName, CommonVersion deploymentVersion) {
        if (deploymentName == null) {
            throw new IllegalArgumentException("deploymentName argument can't be null");
        }

        if (deploymentVersion == null) {
            throw new IllegalArgumentException("deploymentVersion argument can't be null");
        }

        if (!deploymentVersion.equals(FILESYSTEM_COMMON_VERSION) && !isSupportDeployments()) {
            return null;
        }

        File[] listOfDeploymentFolders = getDeploymentFolderList();

        String deploymentFolderName = deploymentName;
        if (isSupportVersion() && isSupportDeployments()) {
            deploymentFolderName = getDeploymentFolderName(deploymentName, deploymentVersion);
        }

        for (File deploymentFolder : listOfDeploymentFolders) {
            if (deploymentFolder.getName().equals(deploymentFolderName)) {
                LocalFolderAPI localFolderAPI = new LocalFolderAPI(deploymentFolder,
                    new ArtefactPathImpl(deploymentFolder.getName()),
                    new LocalWorkspaceImpl(null,
                        deploymentFolder.getParentFile(),
                        getLocalWorkspaceFolderFilter(),
                        getLocalWorkspaceFileFilter()));
                Deployment deployment = new Deployment(localFolderAPI, deploymentName, deploymentVersion);
                return deployment;
            }
        }
        return null;
    }

    private File[] getDeploymentFolderList() {
        File folder = getLoadDeploymentsFromFolder();
        File[] listOfFiles = null;
        if (!isSupportDeployments()) {
            listOfFiles = new File[1];
            listOfFiles[0] = folder;
        } else {
            listOfFiles = folder.listFiles(new FileFilter() {
                @Override public boolean accept(File file) {
                    return file.isDirectory();
                }
            });
        }
        return listOfFiles;
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

    private Object[] getDeploymentNameAndVersionFromFolder(String folderName) {
        Object[] ret = new Object[2];
        int index = folderName.lastIndexOf("_v");
        if (index < 0) {
            if (log.isErrorEnabled()) {
                log.error("Deployment folder \"{}\" in file system data source \"{}\" doesn't have version suffix Deployment was skiped!",
                    folderName, getLoadDeploymentsFromDirectory());
            }
            return null;
        } else {
            String versionSuffix = folderName.substring(index + 2);
            try {
                ret[1] = new CommonVersionImpl(versionSuffix);
            } catch (Exception e) {
                if (log.isErrorEnabled()) {
                    log.error("Deployment folder \"{}\" in file system data source \"{}\" has invalid version suffix. Deployment was skiped!",
                        folderName, getLoadDeploymentsFromDirectory());
                }
                return null;
            }
        }

        ret[0] = folderName.substring(0, index);
        return ret;
    }

    /**
     * {@inheritDoc}
     */
    public Collection<Deployment> getDeployments() {
        Collection<Deployment> deployments = new ArrayList<Deployment>();

        File[] listOfDeploymentFolders = getDeploymentFolderList();

        for (File deploymentFolder : listOfDeploymentFolders) {
            String deploymentName = deploymentFolder.getName();
            CommonVersion commonVersion = FILESYSTEM_COMMON_VERSION;
            if (isSupportVersion() && isSupportDeployments()) {
                Object[] ret = getDeploymentNameAndVersionFromFolder(deploymentFolder.getName());
                if (ret == null){
                    continue;
                }
                deploymentName = (String) ret[0];
                commonVersion = (CommonVersion) ret[1];
            }
            LocalFolderAPI localFolderAPI = new LocalFolderAPI(deploymentFolder,
                new ArtefactPathImpl(deploymentFolder.getName()),
                new LocalWorkspaceImpl(null,
                    deploymentFolder.getParentFile(),
                    getLocalWorkspaceFolderFilter(),
                    getLocalWorkspaceFileFilter()));
            Deployment deployment = new Deployment(localFolderAPI, deploymentName, commonVersion);
            validateDeployment(deployment, deploymentFolder);
            deployments.add(deployment);
        }
        return Collections.unmodifiableCollection(deployments);
    }

    private void validateDeployment(Deployment deployment, File deploymentFolder) {
        if (deployment.getProjects().isEmpty()) {
            if (log.isWarnEnabled()) {
                log.warn("Deployment in file system data source \"{}\" does not contain projects. Make sure that you have specified correct folder!",
                    deploymentFolder);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void addListener(DataSourceListener dataSourceListener) {
        if (dataSourceListener == null) {
            throw new IllegalArgumentException("dataSourceListener argument can't be null");
        }
        synchronized (listeners) {
            listeners.add(dataSourceListener);
            log.info("{} class listener is registered in file system data source", dataSourceListener.getClass());
        }

    }

    /**
     * {@inheritDoc}
     */
    public void removeListener(DataSourceListener dataSourceListener) {
        if (dataSourceListener == null) {
            throw new IllegalArgumentException("dataSourceListener argument can't be null");
        }
        synchronized (listeners) {
            listeners.remove(dataSourceListener);
            log.info("{} class listener is unregistered from file system data source", dataSourceListener.getClass());
        }
    }

    public boolean isSupportDeployments() {
        return supportDeployments;
    }

    public void setSupportDeployments(boolean supportDeployments) {
        this.supportDeployments = supportDeployments;
    }

    public boolean isSupportVersion() {
        return supportVersion;
    }

    public void setSupportVersion(boolean supportVersion) {
        this.supportVersion = supportVersion;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        File folder = getLoadDeploymentsFromFolder();
        validateFileSystemDataSourceFolder(folder);
    }
}
