package org.openl.rules.workspace.uw.impl;

import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;

import org.openl.rules.workspace.abstracts.ArtefactPath;
import org.openl.rules.workspace.abstracts.ProjectArtefact;
import org.openl.rules.workspace.abstracts.ProjectException;
import org.openl.rules.workspace.abstracts.ProjectVersion;
import org.openl.rules.workspace.abstracts.VersionInfo;
import org.openl.rules.workspace.dtr.RepositoryProjectArtefact;
import org.openl.rules.workspace.dtr.impl.RepositoryProjectVersionImpl;
import org.openl.rules.workspace.dtr.impl.RepositoryVersionInfoImpl;
import org.openl.rules.workspace.lw.LocalProjectArtefact;
import org.openl.rules.workspace.props.Property;
import org.openl.rules.workspace.props.PropertyException;
import org.openl.rules.workspace.props.PropertyTypeException;
import org.openl.rules.workspace.repository.RulesRepositoryArtefact;
import org.openl.rules.workspace.uw.UserWorkspaceProjectArtefact;

public abstract class UserWorkspaceProjectArtefactImpl implements UserWorkspaceProjectArtefact {
    private UserWorkspaceProjectImpl project;
    
    private LocalProjectArtefact localArtefact;
    private RepositoryProjectArtefact dtrArtefact;
    
    protected UserWorkspaceProjectArtefactImpl(UserWorkspaceProjectImpl project, LocalProjectArtefact localArtefact, RepositoryProjectArtefact dtrArtefact) {
        this.project = project;

        updateArtefact(localArtefact, dtrArtefact);
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
            VersionInfo vi = new RepositoryVersionInfoImpl(null, getProject().getUser().getUserId());
            ProjectVersion pv = new RepositoryProjectVersionImpl(0, 0, 0, vi);
            
            Collection<ProjectVersion> result = new LinkedList<ProjectVersion>();
            result.add(pv);
            
            return result;
        }
    }

    public Date getEffectiveDate() {
        RulesRepositoryArtefact rra = (RulesRepositoryArtefact) getArtefact();
        return rra.getEffectiveDate();
    }
    
    public Date getExpirationDate() {
        RulesRepositoryArtefact rra = (RulesRepositoryArtefact) getArtefact();
        return rra.getExpirationDate();
    }
    
    public String getLineOfBusiness() {
        RulesRepositoryArtefact rra = (RulesRepositoryArtefact) getArtefact();
        return rra.getLineOfBusiness();
    }
    
    public void setEffectiveDate(Date date) throws ProjectException {
        if (isLocal()) {
            localArtefact.setEffectiveDate(date);
        } else {
            throw new ProjectException("Cannot set effectiveDate in read mode");
        }
    }
    
    public void setExpirationDate(Date date) throws ProjectException {
        if (isLocal()) {
            localArtefact.setExpirationDate(date);
        } else {
            throw new ProjectException("Cannot set expirationDate in read mode");
        }
    }

    public void setLineOfBusiness(String lineOfBusiness) throws ProjectException {
        if (isLocal()) {
            localArtefact.setLineOfBusiness(lineOfBusiness);
        } else {
            throw new ProjectException("Cannot set LOB in read mode");
        }
    }
    
    // --- protected
    
    protected void updateArtefact(LocalProjectArtefact localArtefact, RepositoryProjectArtefact dtrArtefact) {
        this.localArtefact = localArtefact;
        this.dtrArtefact = dtrArtefact;
    }
    
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
