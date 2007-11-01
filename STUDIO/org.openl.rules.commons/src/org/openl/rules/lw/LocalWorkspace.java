package org.openl.rules.lw;

import org.openl.rules.commons.projects.ProjectsContainer;
import org.openl.rules.commons.projects.Project;
import org.openl.rules.commons.projects.ProjectException;

public interface LocalWorkspace extends ProjectsContainer<LocalProject> {
    LocalProject addProject(Project project) throws ProjectException;
    void removeProject(String name) throws ProjectException;

    void refresh();
    void saveAll();
    void release();
}
