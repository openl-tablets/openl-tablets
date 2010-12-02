package org.openl.rules.project.abstraction;

import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.openl.rules.project.impl.ProjectArtefactAPI;
import org.openl.rules.workspace.abstracts.ArtefactPath;
import org.openl.rules.workspace.abstracts.ProjectException;

public class AProjectFolder extends AProjectArtefact {
	private Map<String, AProjectArtefact> artefacts;

	public AProjectFolder(ProjectArtefactAPI api, AProject project) {
		super(api, project);
	}

	public AProjectArtefact getArtefact(String name) throws ProjectException {
		return getArtefactsInternal().get(name);
	}

	public boolean hasArtefact(String name) {
		return getArtefactsInternal().containsKey(name);
	}

	public AProjectArtefact getArtefactByPath(ArtefactPath artefactPath)
			throws ProjectException {
		AProjectArtefact artefact = this;
		for (String s : artefactPath.getSegments()) {
			if (artefact.isFolder()) {
				artefact = ((AProjectFolder) artefact).getArtefact(s);
			} else {
				throw new ProjectException("Wrong path!");
			}
		}
		return artefact;
	}

	public AProjectFolder addFolder(String name) throws ProjectException {
		AProjectFolder createdFolder = new AProjectFolder(impl.addFolder(name),
				getProject());
		getArtefactsInternal().put(name, createdFolder);
		return createdFolder;
	}

	public AProjectResource addResource(String name, AProjectResource resource)
			throws ProjectException {
		AProjectResource createdResource = new AProjectResource(
				impl.addResource(name, resource.getContent()), getProject());
		getArtefactsInternal().put(name, createdResource);
		return createdResource;
	}

	public AProjectResource addResource(String name, InputStream content)
			throws ProjectException {
		AProjectResource createdResource = new AProjectResource(
				impl.addResource(name, content), getProject());
		getArtefactsInternal().put(name, createdResource);
		return createdResource;
	}

	public Collection<AProjectArtefact> getArtefacts() {
		return getArtefactsInternal().values();
	}

	@Override
	public boolean isFolder() {
		return true;
	}

	@Override
	public void update(AProjectArtefact newFolder) throws ProjectException {
		super.update(newFolder);

		AProjectFolder folder = (AProjectFolder) newFolder;
		// remove absent
		for (AProjectArtefact artefact : getArtefacts()) {
			String name = artefact.getName();

			if (!folder.hasArtefact(name)) {
				// was deleted
				artefact.delete();
			} else {
				AProjectArtefact newArtefact = folder.getArtefact(name);

				if (newArtefact.isFolder() == artefact.isFolder()) {
					// update existing
					artefact.update(newArtefact);
				} else {
					// the same name but other type
					artefact.delete();
				}
			}
		}

		// add new
		for (AProjectArtefact artefact : folder.getArtefacts()) {
			String name = artefact.getName();
			if (!hasArtefact(name)) {
				if (artefact.isFolder()) {
					addFolder(name).update(artefact);
				} else {
					addResource(name, (AProjectResource) artefact).update(artefact);
				}
			}
		}
	}

	private Map<String, AProjectArtefact> getArtefactsInternal() {
		if (artefacts == null) {
			artefacts = new HashMap<String, AProjectArtefact>();
			for (ProjectArtefactAPI artefactAPI : impl.getArtefacts()) {
				if (artefactAPI.isFolder()) {
					artefacts.put(artefactAPI.getName(), new AProjectFolder(
							artefactAPI, getProject()));
				} else {
					artefacts.put(artefactAPI.getName(), new AProjectResource(
							artefactAPI, getProject()));
				}
			}
		}
		return artefacts;
	}

	@Override
	public void refresh() {
		super.refresh();
		artefacts = null;
	}
}
