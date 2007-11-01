package org.openl.rules.commons.projects;

import java.io.InputStream;

public interface ProjectResource extends ProjectArtefact {
    InputStream getContent() throws ProjectException;

    String getResourceType();
}
