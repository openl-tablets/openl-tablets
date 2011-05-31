package org.openl.rules.project.abstraction;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.openl.rules.common.ProjectException;
import org.openl.rules.repository.api.ArtefactAPI;
import org.openl.rules.repository.api.FolderAPI;
import org.openl.rules.repository.api.ResourceAPI;

/**
 * Class representing deployment from ProductionRepository.
 * Deployment is set of logically grouped rules projects. 
 * 
 * @author PUdalau
 */
public class Deployment extends AProject {
    private Map<String, AProject> projects;

    public Deployment(FolderAPI api) {
        super(api);
        init();
    }

    protected void init() {
        refresh();
        projects = new HashMap<String, AProject>();
        Map<String, AProjectArtefact> artefacts = new HashMap<String, AProjectArtefact>();
        for (ArtefactAPI artefactAPI : getAPI().getArtefacts()) {
            if (artefactAPI.isFolder()) {
                AProject project = new AProject((FolderAPI) artefactAPI);
                projects.put(artefactAPI.getName(), project);
                artefacts.put(artefactAPI.getName(), project);
            } else {
                artefacts.put(artefactAPI.getName(), new AProjectResource((ResourceAPI) artefactAPI, getProject()));
            }
        }
        setArtefactsInternal(artefacts);
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
