package org.openl.rules.workspace.uw.impl;

import static org.openl.rules.security.Privileges.PRIVILEGE_EDIT;
import static org.openl.rules.security.SecurityUtils.isGranted;

import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.Map;

import org.openl.rules.workspace.abstracts.ArtefactPath;
import org.openl.rules.workspace.abstracts.ProjectArtefact;
import org.openl.rules.workspace.abstracts.ProjectException;
import org.openl.rules.workspace.abstracts.ProjectVersion;
import org.openl.rules.workspace.dtr.RepositoryProjectArtefact;
import org.openl.rules.workspace.lw.LocalProjectArtefact;
import org.openl.rules.workspace.props.Property;
import org.openl.rules.workspace.props.PropertyException;
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

    public void addProperty(Property property) throws PropertyException {
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
            // no versions
            return new LinkedList<ProjectVersion>();
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

    public Map<String, Object> getProps() {
        RulesRepositoryArtefact rra = (RulesRepositoryArtefact) getArtefact();
        return rra.getProps();
    }
    
    public boolean getCanModify() {
        return (!isReadOnly() && isGranted(PRIVILEGE_EDIT));
    }

    public void setEffectiveDate(Date date) throws ProjectException {
        if (isReadOnly()) {
            throw new ProjectException("Cannot set effectiveDate in read mode");
        } else {
            localArtefact.setEffectiveDate(date);
        }
    }

    public void setExpirationDate(Date date) throws ProjectException {
        if (isReadOnly()) {
            throw new ProjectException("Cannot set expirationDate in read mode");
        } else {
            localArtefact.setExpirationDate(date);
        }
    }

    public void setLineOfBusiness(String lineOfBusiness) throws ProjectException {
        if (isReadOnly()) {
            throw new ProjectException("Cannot set LOB in read mode");
        } else {
            localArtefact.setLineOfBusiness(lineOfBusiness);
        }
    }

    public void setProps(Map<String, Object> props) throws ProjectException {
        if (isReadOnly()) {
            throw new ProjectException("Cannot set properties in read mode");
        } else {
            localArtefact.setProps(props);
        }
    }
    
    // --- protected

    protected void updateArtefact(LocalProjectArtefact localArtefact, RepositoryProjectArtefact dtrArtefact) {
        this.localArtefact = localArtefact;
        this.dtrArtefact = dtrArtefact;
    }

    protected ProjectArtefact getArtefact() {
        return (isLocal()) ? localArtefact : dtrArtefact;
    }

    protected boolean isLocal() {
        return project.isLocal();
    }

    public boolean isReadOnly() {
        return project.isReadOnly();
    }

    public UserWorkspaceProjectImpl getProject() {
        return project;
    }

    // can be used in constructor of UserWorkspaceProjectImpl only
    protected void setProject(UserWorkspaceProjectImpl project) {
        this.project = project;
    }
}
