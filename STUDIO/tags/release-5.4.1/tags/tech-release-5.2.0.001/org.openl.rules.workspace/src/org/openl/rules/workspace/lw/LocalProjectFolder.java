package org.openl.rules.workspace.lw;

import java.util.Collection;

import org.openl.rules.workspace.abstracts.ProjectException;
import org.openl.rules.workspace.abstracts.ProjectFolder;
import org.openl.rules.workspace.abstracts.ProjectResource;

public interface LocalProjectFolder extends ProjectFolder, LocalProjectArtefact{
    Collection<LocalProjectArtefact> getArtefacts();

    LocalProjectFolder addFolder(String name) throws ProjectException;
    LocalProjectResource addResource(String name, ProjectResource resource) throws ProjectException;
}
