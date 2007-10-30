package org.openl.rules.commons.artefacts;

import java.util.Collection;

public interface Workspace extends Artefact {
    boolean hasProject(String name);
    Project getProject(String name) throws ArtefactException;
    Collection<Project> getProjects() throws ArtefactException;

    ArtefactPath getArtefactPath(Artefact artefact) throws ArtefactException;
    Artefact findArtefactByPath(ArtefactPath artefactPath) throws ArtefactException;
}
