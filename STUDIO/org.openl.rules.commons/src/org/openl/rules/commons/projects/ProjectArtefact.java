package org.openl.rules.commons.projects;

import org.openl.rules.commons.artefacts.ArtefactPath;
import org.openl.rules.commons.props.PropertiesContainer;

public interface ProjectArtefact extends PropertiesContainer {
    String getName();
    ArtefactPath getArtefactPath();

    ProjectArtefact getArtefact(String name) throws ProjectException;
}
