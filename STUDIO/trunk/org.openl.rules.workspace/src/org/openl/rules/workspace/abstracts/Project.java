package org.openl.rules.workspace.abstracts;


import java.util.Collection;

public interface Project extends ProjectFolder {
    ProjectArtefact getArtefactByPath(ArtefactPath artefactPath) throws ProjectException;

    // current version
    ProjectVersion getVersion();

    Collection<ProjectDependency> getDependencies();

    void setDependencies(Collection<ProjectDependency> dependencies) throws ProjectException;
}
