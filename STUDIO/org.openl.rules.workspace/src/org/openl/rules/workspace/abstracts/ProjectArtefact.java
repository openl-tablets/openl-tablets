package org.openl.rules.workspace.abstracts;

import org.openl.rules.workspace.props.PropertiesContainer;

public interface ProjectArtefact extends PropertiesContainer {
    String getName();
    ArtefactPath getArtefactPath();

    ProjectArtefact getArtefact(String name) throws ProjectException;
    boolean hasArtefact(String name);
    
    boolean isFolder();
}
