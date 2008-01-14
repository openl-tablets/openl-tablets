package org.openl.rules.workspace.abstracts;


import java.util.Collection;

public interface ProjectsContainer<T extends Project> {
    Collection<T> getProjects();
    T getProject(String name) throws ProjectException;
    boolean hasProject(String name);

    ProjectArtefact getArtefactByPath(ArtefactPath artefactPath) throws ProjectException;
}
