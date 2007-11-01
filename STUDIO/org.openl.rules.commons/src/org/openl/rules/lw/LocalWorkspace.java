package org.openl.rules.lw;

import org.openl.rules.commons.projects.Project;
import org.openl.rules.commons.projects.ProjectException;
import org.openl.rules.commons.projects.ProjectsContainer;

import java.io.File;

public interface LocalWorkspace extends ProjectsContainer<LocalProject> {
    LocalProject addProject(Project project) throws ProjectException;
    void removeProject(String name) throws ProjectException;

    void refresh();
    void saveAll();
    void release();

    File getLocation();
}
