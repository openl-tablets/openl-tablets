package org.openl.ruleservice.loader;

import java.io.File;
import java.io.FileFilter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.SmartProps;
import org.openl.rules.common.ProjectException;
import org.openl.rules.common.impl.ArtefactPathImpl;
import org.openl.rules.project.abstraction.Deployment;
import org.openl.rules.project.impl.local.LocalFolderAPI;
import org.openl.rules.workspace.lw.impl.FolderHelper;
import org.openl.rules.workspace.lw.impl.LocalWorkspaceImpl;

/**
 * Local temporary storage on file system for deployments. Reads properties from
 * <i>rules-production.properties</i> file. Clears all data on first
 * initialization.
 * 
 * @author MKamalov
 * 
 */
public class LocalTemporaryDeploymentsStorage {

	private static Log log = LogFactory
			.getLog(LocalTemporaryDeploymentsStorage.class);

	private static String RULES_PRODUCTION_PROPERTIES = "rules-production.properties";

	private static String DEPLOYMENTS_TMP_DIRECTORY = "ruleservice.tmp.dir";

	private static String DEPLOYMENTS_TMP_DIRECTORY_DEFAULT = "/tmp/rules-deploy";

	private File folderToLoadDeploymentsIn;

	private String directoryToLoadDeploymentsIn;

	private static Object flag = new Object();

	private FileFilter localWorkspaceFolderFilter;
	private FileFilter localWorkspaceFileFilter;

	public LocalTemporaryDeploymentsStorage() {
	}

	public LocalTemporaryDeploymentsStorage(
			FileFilter localWorkspaceFolderFilter,
			FileFilter localWorkspaceFileFilter) {
		this.localWorkspaceFolderFilter = localWorkspaceFolderFilter;
		this.localWorkspaceFileFilter = localWorkspaceFileFilter;
	}

	public void setLocalWorkspaceFileFilter(FileFilter localWorkspaceFileFilter) {
		this.localWorkspaceFileFilter = localWorkspaceFileFilter;
	}

	public FileFilter getLocalWorkspaceFileFilter() {
		return localWorkspaceFileFilter;
	}

	public void setLocalWorkspaceFolderFilter(
			FileFilter localWorkspaceFolderFilter) {
		this.localWorkspaceFolderFilter = localWorkspaceFolderFilter;
	}

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
	protected String getDirectoryToLoadDeploymentsIn() {
		SmartProps props = new SmartProps(RULES_PRODUCTION_PROPERTIES);
		String value = props.getStr(DEPLOYMENTS_TMP_DIRECTORY);
		if (value == null || value.trim().length() == 0) {
			return DEPLOYMENTS_TMP_DIRECTORY_DEFAULT;
		}

		return value;
	}

	/**
	 * Generate folder name for given deployment
	 * 
	 * @param deployment
	 * @return folder name
	 */
	protected String getDeploymentFolderName(Deployment deployment) {
		return String.format("%s_v%s", deployment.getName(), deployment
				.getVersion().getVersionName());
	}

	private File getFolderToLoadDeploymentsIn() {
		if (folderToLoadDeploymentsIn == null) {
			synchronized (flag) {
				if (folderToLoadDeploymentsIn == null) {
					if (directoryToLoadDeploymentsIn == null) {
						directoryToLoadDeploymentsIn = getDirectoryToLoadDeploymentsIn();
					}

					folderToLoadDeploymentsIn = new File(
							directoryToLoadDeploymentsIn);

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

	private File getDeploymentFolder(Deployment deployment) {
		File deploymentFolder = new File(getFolderToLoadDeploymentsIn(),
				getDeploymentFolderName(deployment));
		return deploymentFolder;
	}

	/**
	 * Get deployment from local file system
	 * 
	 * @param deployment
	 * @return loaded deployment
	 */
	public Deployment getDeployment(Deployment deployment) {
		if (deployment == null)
			throw new IllegalArgumentException();
		if (containsDeployment(deployment)) {
			File deploymentFolder = getDeploymentFolder(deployment);
			LocalFolderAPI localFolderAPI = new LocalFolderAPI(
					deploymentFolder, new ArtefactPathImpl(
							deploymentFolder.getName()),
					new LocalWorkspaceImpl(null, deploymentFolder
							.getParentFile(), getLocalWorkspaceFolderFilter(),
							getLocalWorkspaceFileFilter()));
			return new Deployment(localFolderAPI);
		} else {
			return loadDeployment(deployment);
		}
	}

	/**
	 * Load deployment to local file system from repository
	 * 
	 * @param deployment
	 * @return loaded deployment
	 */
	public Deployment loadDeployment(Deployment deployment) {
		if (deployment == null)
			throw new IllegalArgumentException();
		File deploymentFolder = getDeploymentFolder(deployment);
		LocalFolderAPI localFolderAPI = new LocalFolderAPI(deploymentFolder,
				new ArtefactPathImpl(deploymentFolder.getName()),
				new LocalWorkspaceImpl(null, deploymentFolder.getParentFile(),
						getLocalWorkspaceFolderFilter(),
						getLocalWorkspaceFileFilter()));
		Deployment loadedDeployment = new Deployment(localFolderAPI);
		try {
			loadedDeployment.update(deployment, null, deployment
					.getCommonVersion().getMajor(), deployment
					.getCommonVersion().getMinor());
			loadedDeployment.refresh();
		} catch (ProjectException e) {
			// FIXME
			// e.printStackTrace();
			throw new RuntimeException(e);
		}
		return loadedDeployment;
	}

	/**
	 * Remove deployment to local file system from repository
	 * 
	 * @param deployment
	 * @return true if and only if the file or directory is successfully
	 *         deleted; false otherwise
	 */
	public boolean removeDeployment(Deployment deployment) {
		if (deployment == null)
			throw new IllegalArgumentException();
		return FolderHelper.clearFolder(getDeploymentFolder(deployment));
	}

	/**
	 * Check to existing deployment in local temporary folder
	 * 
	 * @param deployment
	 * @return true if and only if the deployment exists; false otherwise
	 */
	public boolean containsDeployment(Deployment deployment) {
		return getDeploymentFolder(deployment).exists();
	}

	/**
	 * Clear local temporary storage
	 */
	public void clear() {
		synchronized (flag) {
			FolderHelper.clearFolder(getFolderToLoadDeploymentsIn());
		}
	}

}
