package org.openl.rules.lw;

import org.openl.rules.commons.artefacts.ArtefactPath;
import org.openl.rules.commons.projects.Project;
import org.openl.rules.commons.projects.ProjectException;

public interface LocalProject extends Project, LocalProjectFolder{
    public LocalProjectArtefact getArtefactByPath(ArtefactPath artefactPath) throws ProjectException;

    void load();
    void save();
}
