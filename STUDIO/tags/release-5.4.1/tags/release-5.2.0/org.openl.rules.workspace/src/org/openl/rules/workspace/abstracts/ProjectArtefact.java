package org.openl.rules.workspace.abstracts;

import org.openl.rules.workspace.props.PropertiesContainer;

public interface ProjectArtefact extends PropertiesContainer {
    ProjectArtefact getArtefact(String name) throws ProjectException;

    ArtefactPath getArtefactPath();

    String getName();

    boolean hasArtefact(String name);

    boolean isFolder();
}
