package org.openl.rules.workspace.abstracts;

import java.io.InputStream;

public interface ProjectResource extends ProjectArtefact {
    InputStream getContent() throws ProjectException;

    String getResourceType();
}
