package org.openl.rules.workspace.lw;

import org.openl.rules.workspace.abstracts.ArtefactPath;
import org.openl.rules.workspace.abstracts.Project;
import org.openl.rules.workspace.abstracts.ProjectException;
import org.openl.rules.workspace.abstracts.ProjectVersion;

public interface LocalProject extends Project, LocalProjectFolder {
    /**
     * Local project was checked in successfully. LocalProject must reset all
     * isNew/isChanged flags.
     *
     * @param newVersion new version of project in the Design Time Repository
     */
    void checkedIn(ProjectVersion newVersion);

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
