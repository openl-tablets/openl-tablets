package org.openl.rules.dtr.impl;

import org.openl.rules.WorkspaceUser;
import org.openl.rules.commons.artefacts.ArtefactPath;
import org.openl.rules.commons.projects.Project;
import org.openl.rules.commons.projects.ProjectArtefact;
import org.openl.rules.commons.projects.ProjectException;
import org.openl.rules.commons.projects.ProjectVersion;
import org.openl.rules.dtr.DesignTimeRepository;
import org.openl.rules.dtr.RepositoryException;
import org.openl.rules.dtr.RepositoryProject;

import java.util.Collection;
import java.util.HashMap;

public class DesignTimeRepositoryImpl implements DesignTimeRepository {
    private HashMap<String, RepositoryProject> projects;

    public DesignTimeRepositoryImpl() {
        projects = new HashMap<String, RepositoryProject>();
    }

    public Collection<RepositoryProject> getProjects() {
        return projects.values();
    }

    public RepositoryProject getProject(String name) throws RepositoryException {
        RepositoryProject rp = projects.get(name);
        if (rp == null) {
            throw new RepositoryException("Cannot find project ''{0}''", name);
        }
        return rp;
    }

    public boolean hasProject(String name) {
        return (projects.get(name) != null);
    }

    public ProjectArtefact getArtefactByPath(ArtefactPath artefactPath) throws ProjectException {
        String projectName = artefactPath.segment(0);
        RepositoryProject rp = getProject(projectName);

        ArtefactPath pathInProject = artefactPath.getRelativePath(1);
        return rp.getArtefactByPath(pathInProject);
    }

    public RepositoryProject getProjectVersion(String name, ProjectVersion version) throws RepositoryException {
        return getProject(name);
    }

    public void updateProject(Project project, WorkspaceUser user) throws RepositoryException {
    }

    public void copyProject(Project project, String name) throws RepositoryException {
    }
}
