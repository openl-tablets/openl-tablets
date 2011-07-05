package org.openl.rules.ruleservice.loader;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.List;

import org.openl.rules.common.CommonVersion;
import org.openl.rules.common.impl.ArtefactPathImpl;
import org.openl.rules.project.abstraction.Deployment;
import org.openl.rules.project.impl.local.LocalFolderAPI;
import org.openl.rules.workspace.lw.impl.LocalWorkspaceImpl;

/**
 * File based data source.
 * @author MKamalov
 *
 */
public class FileSystemDataSource implements IDataSource {
    
    //private Log log = LogFactory.getLog(FileSystemDataSource.class);

    private String loadDeploymentsFromDirectory;

    private File loadDeploymentsFromFolder;

    private FileFilter localWorkspaceFolderFilter;

    private FileFilter localWorkspaceFileFilter;

    private Object flag = new Object();

    /**
     * Sets localWorkspaceFileFilter @see LocalFolderAPI. Spring bean
     * configuration property
     * 
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
     * Sets localWorkspaceFolderFilter @see LocalFolderAPI. Spring bean
     * configuration property
     * 
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
                }
            }
        }
        return loadDeploymentsFromFolder;
    }

    /** {@inheritDoc} */
    public Deployment getDeployment(String deploymentName, CommonVersion deploymentVersion) {
        if (deploymentName == null){
            throw new IllegalArgumentException("deploymentName argument can't be null");
        }
        
        if (deploymentVersion == null){
            throw new IllegalArgumentException("deploymentVersion argument can't be null");
        }
        
        File loadDeploymentsFromFolder = getLoadDeploymentsFromFolder();
        if (!loadDeploymentsFromFolder.exists()) {
            throw new DataSourceException("Folder doesn't exist. Path: " + getLoadDeploymentsFromDirectory());
        }
        if (!loadDeploymentsFromFolder.isDirectory()) {
            throw new DataSourceException("Folder doesn't exist. Path: " + getLoadDeploymentsFromDirectory());
        }
        if (loadDeploymentsFromFolder.getName().equals(deploymentName)) {
            LocalFolderAPI localFolderAPI = new LocalFolderAPI(loadDeploymentsFromFolder, new ArtefactPathImpl(
                    loadDeploymentsFromFolder.getName()), new LocalWorkspaceImpl(null,
                    loadDeploymentsFromFolder.getParentFile(), getLocalWorkspaceFolderFilter(),
                    getLocalWorkspaceFileFilter()));
            Deployment deployment = new Deployment(localFolderAPI);
            return deployment;
        } else {
            return null;
        }
    }
    
    /** {@inheritDoc} */
    public List<Deployment> getDeployments() {
        File loadDeploymentsFromFolder = getLoadDeploymentsFromFolder();
        if (!loadDeploymentsFromFolder.exists()) {
            throw new DataSourceException("Folder doesn't exist. Path: " + getLoadDeploymentsFromDirectory());
        }
        if (!loadDeploymentsFromFolder.isDirectory()) {
            throw new DataSourceException("Folder doesn't exist. Path: " + getLoadDeploymentsFromDirectory());
        }
        List<Deployment> deployments = new ArrayList<Deployment>(1);
        LocalFolderAPI localFolderAPI = new LocalFolderAPI(loadDeploymentsFromFolder, new ArtefactPathImpl(
                loadDeploymentsFromFolder.getName()), new LocalWorkspaceImpl(null,
                loadDeploymentsFromFolder.getParentFile(), getLocalWorkspaceFolderFilter(),
                getLocalWorkspaceFileFilter()));
        Deployment deployment = new Deployment(localFolderAPI);
        deployments.add(deployment);
        return deployments;
    }

    /**
     * Listeners aren't supported by this data source
     */
    public List<IDataSourceListener> getListeners() {
        throw new UnsupportedOperationException();
    }

    /**
     * Listeners aren't supported by this data source
     */
    public void addListener(IDataSourceListener dataSourceListener) {
        throw new UnsupportedOperationException();
    }

    /**
     * Listeners aren't supported by this data source
     */
    public void removeAllListeners() {
        throw new UnsupportedOperationException();
    }

    /**
     * Listeners aren't supported by this data source
     */
    public void removeListener(IDataSourceListener dataSourceListener) {
        throw new UnsupportedOperationException();
    }
}
