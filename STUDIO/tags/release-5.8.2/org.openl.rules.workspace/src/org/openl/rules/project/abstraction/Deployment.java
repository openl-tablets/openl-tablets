package org.openl.rules.project.abstraction;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.openl.rules.common.CommonVersion;
import org.openl.rules.common.ProjectException;
import org.openl.rules.repository.api.ArtefactAPI;
import org.openl.rules.repository.api.FolderAPI;

/**
 * Class representing deployment from ProductionRepository. Deployment is set of
 * logically grouped rules projects.
 * 
 * @author PUdalau
 */
public class Deployment extends AProject {
	private Map<String, AProject> projects;

	private String deploymentName;
	private CommonVersion commonVersion;

	public Deployment(FolderAPI api) {
		super(api);
		init();
	}

	public Deployment(FolderAPI api, String deploymentName, CommonVersion commonVersion) {
		super(api);
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

	protected void init() {
		super.refresh();
		projects = new HashMap<String, AProject>();
		//Map<String, AProjectArtefact> artefacts = new HashMap<String, AProjectArtefact>();
		for (ArtefactAPI artefactAPI : getAPI().getArtefacts()) {
			if (artefactAPI.isFolder()) {
				AProject project = new AProject((FolderAPI) artefactAPI);
				projects.put(artefactAPI.getName(), project);
				//artefacts.put(artefactAPI.getName(), project);
			} /*else {
				artefacts.put(artefactAPI.getName(), new AProjectResource(
						(ResourceAPI) artefactAPI, getProject()));
			}*/
		}
		//setArtefactsInternal(artefacts);
	}

	public Collection<AProject> getProjects() {
		return projects.values();
	}

	public AProject getProject(String name) {
		return projects.get(name);
	}

	public void addProject(AProject project) throws ProjectException {
		String projectName = project.getName();
		addFolder(projectName);
		init();
		getProject(projectName).update(project, null, 0, 0);
	}
}
