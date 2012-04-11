package org.openl.rules.workspace.abstracts;

import java.util.Collection;

public interface Project extends ProjectFolder {
    ProjectArtefact getArtefactByPath(ArtefactPath artefactPath) throws ProjectException;

    Collection<ProjectDependency> getDependencies();

    // current version
    ProjectVersion getVersion();

    void setDependencies(Collection<ProjectDependency> dependencies) throws ProjectException;
}
