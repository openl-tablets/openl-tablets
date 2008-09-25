package org.openl.rules.workspace.lw;

import org.openl.rules.workspace.abstracts.Project;
import org.openl.rules.workspace.abstracts.ProjectException;
import org.openl.rules.workspace.abstracts.ProjectsContainer;

import java.io.File;

public interface LocalWorkspace extends ProjectsContainer<LocalProject> {
    LocalProject addProject(Project project) throws ProjectException;
    void removeProject(String name) throws ProjectException;

    void refresh();
    void saveAll();
    void release();

    File getLocation();

    void addWorkspaceListener(LocalWorkspaceListener listener);
    boolean removeWorkspaceListener(LocalWorkspaceListener listener);
}
