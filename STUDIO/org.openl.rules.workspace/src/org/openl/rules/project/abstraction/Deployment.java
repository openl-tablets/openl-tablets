package org.openl.rules.project.abstraction;

import java.io.File;
import java.util.*;

import org.openl.rules.common.CommonUser;
import org.openl.rules.common.CommonVersion;
import org.openl.rules.common.ProjectException;
import org.openl.rules.common.ProjectVersion;
import org.openl.rules.common.impl.CommonVersionImpl;
import org.openl.rules.common.impl.RepositoryProjectVersionImpl;
import org.openl.rules.common.impl.RepositoryVersionInfoImpl;
import org.openl.rules.project.impl.local.LocalRepository;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.Repository;

/**
 * Class representing deployment from ProductionRepository. Deployment is set of
 * logically grouped rules projects.
 * 
 * @author PUdalau
 */
public class Deployment extends AProjectFolder {
	private static final String SEPARATOR = "#";
	private Map<String, AProject> projects;

	private String deploymentName;
	private CommonVersion commonVersion;

	public Deployment(Repository repository, String folderPath) {
		super(null, repository, folderPath, null);
		init();
	}

	public Deployment(Repository repository, FileData fileData) {
		super(null, repository, fileData.getName(), fileData.getVersion());
		setFileData(fileData);

		String path = fileData.getName();
		String deploymentName = path.substring(path.lastIndexOf("/") + 1);
		int separatorPosition = deploymentName.lastIndexOf(SEPARATOR);

		if (separatorPosition >= 0) {
			this.deploymentName = deploymentName.substring(0, separatorPosition);
			String version = deploymentName.substring(separatorPosition + 1);
			setHistoryVersion(version);
			this.commonVersion = new CommonVersionImpl(version);
		}

		init();
	}

	public Deployment(Repository repository, String folderName, String deploymentName, CommonVersion commonVersion) {
		super(null, repository, folderName, commonVersion.getVersionName());
		init();
		this.commonVersion = commonVersion;
		this.deploymentName = deploymentName;
	}

	public CommonVersion getCommonVersion() {
		if (commonVersion == null) return this.getVersion();
		return commonVersion;
	}

	public String getDeploymentName() {
		if (deploymentName == null) return this.getName();
		return deploymentName;
	}
	
	@Override
	public void refresh() {
		init();
	}

	private void init() {
		super.refresh();
		projects = new HashMap<String, AProject>();

		for (AProjectArtefact artefact : getArtefactsInternal().values()) {
			String projectPath = artefact.getArtefactPath().getStringValue();
			projects.put(artefact.getName(), new AProject(getRepository(), projectPath));
		}
	}

	public Collection<AProject> getProjects() {
		return projects.values();
	}

	public AProject getProject(String name) {
		return projects.get(name);
	}

	@Override
	public ProjectVersion getVersion() {
		RepositoryVersionInfoImpl rvii = new RepositoryVersionInfoImpl(null, null);
		return new RepositoryProjectVersionImpl(commonVersion, rvii);
	}

	@Override
	protected Map<String, AProjectArtefact> createInternalArtefacts() {
		if (getRepository() instanceof LocalRepository) {
			LocalRepository repository = (LocalRepository) getRepository();
			File[] files = new File(repository.getLocation(), getFolderPath()).listFiles();
			Map<String, AProjectArtefact> result = new HashMap<String, AProjectArtefact>();
			if (files != null) {
				for (File file : files) {
					result.put(file.getName(), new AProject(repository, getFolderPath() + "/" + file.getName()));
				}
			}
			return result;
		} else {
			return super.createInternalArtefacts();
		}
	}

	@Override
	public boolean isHistoric() {
		return false;
	}

	@Override
	public void update(AProjectArtefact newFolder, CommonUser user) throws ProjectException {
		Deployment other = (Deployment) newFolder;
		// add new
		for (AProject otherProject : other.getProjects()) {
			String name = otherProject.getName();
			if (!hasArtefact(name)) {
				AProject newProject = new AProject(getRepository(), getFolderPath() + "/" + name);
				newProject.update(otherProject, user);
				projects.put(newProject.getName(), newProject);
			}
		}
	}
}
