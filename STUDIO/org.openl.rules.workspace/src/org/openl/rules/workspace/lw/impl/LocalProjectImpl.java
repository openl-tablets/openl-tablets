package org.openl.rules.workspace.lw.impl;

import org.openl.rules.workspace.abstracts.*;
import org.openl.rules.workspace.lw.LocalProject;
import org.openl.rules.workspace.lw.LocalProjectArtefact;

import java.io.File;
import java.util.Collection;
import java.util.LinkedList;

public class LocalProjectImpl extends LocalProjectFolderImpl implements LocalProject {
    private ProjectVersion version;

    private LocalWorkspaceImpl localWorkspace;

    public LocalProjectImpl(String name, ArtefactPath path, File location, ProjectVersion version, LocalWorkspaceImpl localWorkspace) {
        super(name, path, location);

        this.version = version;
        this.localWorkspace = localWorkspace;
    }

    public LocalProjectArtefact getArtefactByPath(ArtefactPath artefactPath) throws ProjectException {
        LocalProjectArtefact lpa = this;

        for (String s : artefactPath.getSegments()) {
            lpa = lpa.getArtefact(s);
        }

        return lpa;
    }

    public ProjectVersion getVersion() {
        return version;
    }

    public Collection<ProjectDependency> getDependencies() {
        // TODO return valid data
        return new LinkedList<ProjectDependency>();
    }

    public void load() {
        // TODO -- load from File System (props)
        // ...
        refresh();
    }

    public void save() {
        // TODO -- save to File System (props)
        // ...
    }

    public void remove() {
        super.remove();

        localWorkspace.notifyRemoved(this);
    }

    // --- protected

    protected void downloadArtefact(Project project) throws ProjectException {
        super.downloadArtefact(project);

        setNew(false);
        setChanged(false);
    }
}
