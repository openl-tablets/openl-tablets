package org.openl.rules.ruleservice.loader;

import org.openl.rules.common.CommonVersion;
import org.openl.rules.common.impl.ArtefactPathImpl;
import org.openl.rules.project.abstraction.Deployment;
import org.openl.rules.project.impl.local.LocalFolderAPI;
import org.openl.rules.workspace.lw.impl.LocalWorkspaceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * File based data source. Thread safe implementation.
 *
 * @author Marat Kamalov
 */
public class FileSystemDataSource implements DataSource {

    private final Logger log = LoggerFactory.getLogger(FileSystemDataSource.class);

    private String loadDeploymentsFromDirectory;

    private File loadDeploymentsFromFolder;

    private FileFilter localWorkspaceFolderFilter;

    private FileFilter localWorkspaceFileFilter;

    private Object flag = new Object();

    List<DataSourceListener> listeners = new ArrayList<DataSourceListener>();

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
            throw new DataSourceException("File system data source folder \"" + getLoadDeploymentsFromDirectory()
                    + "\"  doesn't exist");
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

        File folder = getLoadDeploymentsFromFolder();
        validateFileSystemDataSourceFolder(folder);
        if (folder.getName().equals(deploymentName)) {
            LocalFolderAPI localFolderAPI = new LocalFolderAPI(folder, new ArtefactPathImpl(folder.getName()),
                    new LocalWorkspaceImpl(null, folder.getParentFile(), getLocalWorkspaceFolderFilter(),
                            getLocalWorkspaceFileFilter()));
            Deployment deployment = new Deployment(localFolderAPI);
            return deployment;
        } else {
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    public Collection<Deployment> getDeployments() {
        File folder = getLoadDeploymentsFromFolder();
        validateFileSystemDataSourceFolder(folder);
        Collection<Deployment> deployments = new ArrayList<Deployment>(1);
        LocalFolderAPI localFolderAPI = new LocalFolderAPI(folder, new ArtefactPathImpl(folder.getName()),
                new LocalWorkspaceImpl(null, folder.getParentFile(), getLocalWorkspaceFolderFilter(),
                        getLocalWorkspaceFileFilter()));
        Deployment deployment = new Deployment(localFolderAPI);
        deployments.add(deployment);
        validateDeployment(deployment);
        return Collections.unmodifiableCollection(deployments);
    }

    private void validateDeployment(Deployment deployment) {
        if (deployment.getProjects().isEmpty()) {
            log.warn(
                    "File system data source folder \"{}\" does not contain projects. Make sure that you have specified correct folder!",
                    getLoadDeploymentsFromDirectory());
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
}
