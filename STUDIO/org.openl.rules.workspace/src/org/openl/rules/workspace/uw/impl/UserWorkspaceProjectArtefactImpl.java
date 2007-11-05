package org.openl.rules.workspace.uw.impl;

import java.util.Collection;

import org.openl.rules.workspace.abstracts.ArtefactPath;
import org.openl.rules.workspace.abstracts.ProjectArtefact;
import org.openl.rules.workspace.abstracts.ProjectVersion;
import org.openl.rules.workspace.dtr.RepositoryProjectArtefact;
import org.openl.rules.workspace.lw.LocalProjectArtefact;
import org.openl.rules.workspace.props.Property;
import org.openl.rules.workspace.props.PropertyException;
import org.openl.rules.workspace.props.PropertyTypeException;
import org.openl.rules.workspace.uw.UserWorkspaceProjectArtefact;

public abstract class UserWorkspaceProjectArtefactImpl implements UserWorkspaceProjectArtefact {
    private UserWorkspaceProjectImpl project;
    
    private LocalProjectArtefact localArtefact;
    private RepositoryProjectArtefact dtrArtefact;
    
    protected UserWorkspaceProjectArtefactImpl(UserWorkspaceProjectImpl project, LocalProjectArtefact localArtefact, RepositoryProjectArtefact dtrArtefact) {
        this.project = project;
        this.localArtefact = localArtefact;
        this.dtrArtefact = dtrArtefact;
    }
    
    public ArtefactPath getArtefactPath() {
        return getArtefact().getArtefactPath();
    }

    public String getName() {
        return getArtefact().getName();
    }

    public void addProperty(Property property) throws PropertyTypeException {
        // TODO check whether it can edit
        getArtefact().addProperty(property);
    }

    public Collection<Property> getProperties() {
        return getArtefact().getProperties();
    }

    public Property getProperty(String name) throws PropertyException {
        return getArtefact().getProperty(name);
    }

    public boolean hasProperty(String name) {
        return getArtefact().hasProperty(name);
    }

    public Property removeProperty(String name) throws PropertyException {
        // TODO check whether it can edit
        return getArtefact().removeProperty(name);
    }

    public Collection<ProjectVersion> getVersions() {
        if (dtrArtefact != null) {
            return dtrArtefact.getVersions();
        } else {
            // FIXME return {0.0.0}
            return null;
        }
    }

    // --- protected
    
    protected ProjectArtefact getArtefact() {
        return (project.isLocal()) ? localArtefact : dtrArtefact;
    }
    
    protected boolean isLocal() {
        return project.isLocal();
    }

    protected UserWorkspaceProjectImpl getProject() {
        return project;
    }

    // can be used in constructor of UserWorkspaceProjectImpl only
    protected void setProject(UserWorkspaceProjectImpl project) {
        this.project = project;
    }
}
