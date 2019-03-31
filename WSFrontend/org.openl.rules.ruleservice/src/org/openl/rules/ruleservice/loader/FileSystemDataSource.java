package org.openl.rules.ruleservice.loader;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.openl.rules.common.CommonVersion;
import org.openl.rules.common.impl.ArtefactPathImpl;
import org.openl.rules.common.impl.CommonVersionImpl;
import org.openl.rules.project.abstraction.Deployment;
import org.openl.rules.project.impl.local.LocalFolderAPI;
import org.openl.rules.repository.exceptions.RRepositoryException;
import org.openl.rules.repository.file.FileSystemRepository;
import org.openl.rules.workspace.lw.impl.LocalWorkspaceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * File based data source. Thread safe implementation.
 *
 * @author Marat Kamalov
 */
public class FileSystemDataSource implements DataSource {

    private final Logger log = LoggerFactory.getLogger(FileSystemDataSource.class);

    private static final CommonVersion FILESYSTEM_COMMON_VERSION = new CommonVersionImpl(0, 0, 1);

    private File loadDeploymentsFromDirectory;

    private FileFilter localWorkspaceFolderFilter;

    private FileFilter localWorkspaceFileFilter;

    private boolean supportDeployments = false;

    private boolean supportVersion = false;

    /**
     * Sets localWorkspaceFileFilter @see LocalFolderAPI. Spring bean
     * configuration property.
     */
    public void setLocalWorkspaceFileFilter(FileFilter localWorkspaceFileFilter) {
        this.localWorkspaceFileFilter = localWorkspaceFileFilter;
    }

    /**
     * Sets localWorkspaceFolderFilter @see LocalFolderAPI. Spring bean
     * configuration property.
     */
    public void setLocalWorkspaceFolderFilter(FileFilter localWorkspaceFolderFilter) {
        this.localWorkspaceFolderFilter = localWorkspaceFolderFilter;
    }

    public void setSupportDeployments(boolean supportDeployments) {
        this.supportDeployments = supportDeployments;
    }

    public void setSupportVersion(boolean supportVersion) {
        this.supportVersion = supportVersion;
    }

    public FileSystemDataSource(File loadDeploymentsFromDirectory) {
        if (loadDeploymentsFromDirectory == null) {
            throw new IllegalArgumentException("loadDeploymentsFromDirectory argument can't be null");
        }
        if (!loadDeploymentsFromDirectory.exists()) {
            if (!loadDeploymentsFromDirectory.mkdirs()) {
                log.warn("Failed to create file system data source folder '{}'!", loadDeploymentsFromDirectory);
            } else {
                log.info("File system data source '{}' has been created successfully!", loadDeploymentsFromDirectory);
            }
        }
        if (!loadDeploymentsFromDirectory.exists() || !loadDeploymentsFromDirectory.isDirectory()) {
            throw new DataSourceException("File system data source folder '" + loadDeploymentsFromDirectory + "' doesn't exist");
        }
        this.loadDeploymentsFromDirectory = loadDeploymentsFromDirectory;
    }

    /**
     * {@inheritDoc}
     */
    public Deployment getDeployment(String deploymentName, CommonVersion deploymentVersion) {
        if (deploymentName == null) {
            throw new IllegalArgumentException("deploymentName argument must not be null!");
        }

        if (deploymentVersion == null) {
            throw new IllegalArgumentException("deploymentVersion argument must not be null!");
        }

        if (!deploymentVersion.equals(FILESYSTEM_COMMON_VERSION) && !supportDeployments) {
            return null;
        }

        File[] listOfDeploymentFolders = getDeploymentFolderList();

        String deploymentFolderName = deploymentName;
        if (supportVersion && supportDeployments) {
            deploymentFolderName = deploymentName + "_v" + deploymentVersion.getVersionName();
        }

        for (File deploymentFolder : listOfDeploymentFolders) {
            if (deploymentFolder.getName().equals(deploymentFolderName)) {
                return getDeployment(deploymentFolder, deploymentName, deploymentVersion);
            }
        }
        return null;
    }

    private File[] getDeploymentFolderList() {
        File folder = loadDeploymentsFromDirectory;
        File[] listOfFiles;
        if (!supportDeployments) {
            listOfFiles = new File[1];
            listOfFiles[0] = folder;
        } else {
            listOfFiles = folder.listFiles(File::isDirectory);
        }
        return listOfFiles;
    }

    /**
     * {@inheritDoc}
     */
    public Collection<Deployment> getDeployments() {
        Collection<Deployment> deployments = new ArrayList<>();

        File[] listOfDeploymentFolders = getDeploymentFolderList();

        for (File deploymentFolder : listOfDeploymentFolders) {
            String deploymentName = deploymentFolder.getName();
            CommonVersion commonVersion = FILESYSTEM_COMMON_VERSION;
            if (supportVersion && supportDeployments) {
                String folderName = deploymentFolder.getName();
                try {
                    int index = folderName.lastIndexOf("_v");
                    String versionSuffix = folderName.substring(index + 2);
                    deploymentName = folderName.substring(0, index);
                    commonVersion = new CommonVersionImpl(versionSuffix);
                } catch (Exception e) {
                    log.error(
                        "Deployment has been skiped! Deployment folder '{}' in file system data source '{}' has invalid version suffix.",
                        folderName,
                        loadDeploymentsFromDirectory);
                    continue;
                }
            }
            Deployment deployment = getDeployment(deploymentFolder, deploymentName, commonVersion);
            if (deployment.getProjects().isEmpty()) {
                log.warn(
                    "Deployment of the file system data source '{}' does not contain projects. Make sure that you have specified correct folder!",
                    deploymentFolder);
            }
            deployments.add(deployment);
        }
        return Collections.unmodifiableCollection(deployments);
    }

    private Deployment getDeployment(File deploymentFolder,
            String deploymentName,
            CommonVersion deploymentVersion) {
        LocalFolderAPI localFolderAPI = new LocalFolderAPI(deploymentFolder,
                new ArtefactPathImpl(deploymentFolder.getName()),
                new LocalWorkspaceImpl(null,
                        deploymentFolder.getParentFile(),
                        localWorkspaceFolderFilter,
                        localWorkspaceFileFilter));
        FileSystemRepository repository = new FileSystemRepository();
        repository.setRoot(deploymentFolder.getParentFile());
        try {
            repository.initialize();
        } catch (RRepositoryException e) {
            log.error("Failed to initialize local repository: {}", e.getMessage(), e);
        }
        String folderPath = localFolderAPI.getArtefactPath().getStringValue();

        // FileSystemDataSource can contain projects stored either as zip or as a folder. Depending on it we construct Deployment object accordingly
        boolean folderStructure = !repository.listFolders(folderPath).isEmpty();

        return new Deployment(repository, folderPath, deploymentName, deploymentVersion, folderStructure);
    }

    /**
     * {@inheritDoc}
     */
    public void setListener(DataSourceListener dataSourceListener) {
    }
}
