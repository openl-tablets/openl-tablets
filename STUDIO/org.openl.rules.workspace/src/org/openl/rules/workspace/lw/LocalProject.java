package org.openl.rules.workspace.lw;

import org.openl.rules.workspace.abstracts.ArtefactPath;
import org.openl.rules.workspace.abstracts.Project;
import org.openl.rules.workspace.abstracts.ProjectException;

public interface LocalProject extends Project, LocalProjectFolder{
    public LocalProjectArtefact getArtefactByPath(ArtefactPath artefactPath) throws ProjectException;

    /**
     * Loads previously saved project and its state.
     */
    void load() throws ProjectException;

    /**
     * Saves project and its state.
     */
    void save() throws ProjectException;
}
