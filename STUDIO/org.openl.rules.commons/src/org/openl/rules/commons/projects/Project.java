package org.openl.rules.commons.projects;

import org.openl.rules.commons.artefacts.ArtefactPath;

import java.util.Collection;

public interface Project extends ProjectFolder {
    ProjectArtefact getArtefactByPath(ArtefactPath artefactPath) throws ProjectException;

    // current version
    ProjectVersion getVersion();

    Collection<ProjectDependency> getDependencies();
}
