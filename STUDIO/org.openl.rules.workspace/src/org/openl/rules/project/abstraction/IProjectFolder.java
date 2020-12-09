package org.openl.rules.project.abstraction;

import org.openl.rules.common.ArtefactPath;
import org.openl.rules.common.ProjectException;

public interface IProjectFolder extends IProjectArtefact {

    String getFolderPath();
    IProjectArtefact getArtefact(String name) throws ProjectException;
    ArtefactPath getArtefactPath();

}
