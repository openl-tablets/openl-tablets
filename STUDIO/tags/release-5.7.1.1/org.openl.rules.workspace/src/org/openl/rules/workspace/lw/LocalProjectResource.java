package org.openl.rules.workspace.lw;

import java.io.InputStream;

import org.openl.rules.workspace.abstracts.ProjectException;
import org.openl.rules.workspace.abstracts.ProjectResource;

public interface LocalProjectResource extends ProjectResource, LocalProjectArtefact {
    void setContent(InputStream inputStream) throws ProjectException;
}
