package org.openl.rules.commons.artefacts;

public interface Project extends Artefact {
    Workspace getWorkspace() throws ArtefactException;
    
    ArtefactPath getArtefactPath(Artefact artefact) throws ArtefactException;
    Artefact findArtefactByPath(ArtefactPath artefactPath) throws ArtefactException;
}
