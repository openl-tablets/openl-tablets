package org.openl.rules.project.abstraction;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.openl.rules.project.impl.ProjectArtefactAPI;
import org.openl.rules.workspace.abstracts.ArtefactPath;
import org.openl.rules.workspace.abstracts.ProjectException;
import org.openl.rules.workspace.abstracts.ProjectVersion;
import org.openl.rules.workspace.dtr.impl.RepositoryProjectVersionImpl;
import org.openl.rules.workspace.props.PropertiesContainer;
import org.openl.rules.workspace.props.Property;
import org.openl.rules.workspace.props.PropertyException;
import org.openl.rules.workspace.repository.RulesRepositoryArtefact;
import static org.openl.rules.security.Privileges.*;
import static org.openl.rules.security.SecurityUtils.isGranted;

public class AProjectArtefact implements PropertiesContainer, RulesRepositoryArtefact {
    protected ProjectArtefactAPI impl;
    private AProject project;

    public AProjectArtefact(ProjectArtefactAPI api, AProject project) {
        this.impl = api;
        this.project = project;
    }

    public AProject getProject() {
        return project;
    }

    public ProjectArtefactAPI getAPI() {
        return impl;
    }

    public void setAPI(ProjectArtefactAPI impl) {
        this.impl = impl;
    }

    public Date getEffectiveDate() {
        return impl.getEffectiveDate();
    }

    public Date getExpirationDate() {
        return impl.getExpirationDate();
    }

    public String getLineOfBusiness() {
        return impl.getLineOfBusiness();
    }

    public Map<String, Object> getProps() {
        return impl.getProps();
    }

    public void setEffectiveDate(Date date) throws ProjectException {
        impl.setEffectiveDate(date);
    }

    public void setExpirationDate(Date date) throws ProjectException {
        impl.setExpirationDate(date);
    }

    public void setLineOfBusiness(String lineOfBusiness) throws ProjectException {
        impl.setLineOfBusiness(lineOfBusiness);
    }

    public void setProps(Map<String, Object> props) throws ProjectException {
        impl.setProps(props);
    }

    public void addProperty(Property property) throws PropertyException {
        impl.addProperty(property);
    }

    public Collection<Property> getProperties() {
        return impl.getProperties();
    }

    public Property getProperty(String name) throws PropertyException {
        return impl.getProperty(name);
    }

    public boolean hasProperty(String name) {
        return impl.hasProperty(name);
    }

    public Property removeProperty(String name) throws PropertyException {
        return impl.removeProperty(name);
    }

    public void delete() throws ProjectException {
        impl.delete(null);
    }

    public ArtefactPath getArtefactPath() {
        return impl.getArtefactPath();
    }

    public String getName() {
        return impl.getName();
    }

    public boolean isFolder() {
        return impl.isFolder();
    }

    // current version
    public ProjectVersion getVersion() {
        return impl.getVersion();
    }
    
    public ProjectVersion getLastVersion() {
        List<ProjectVersion> versions = getVersions();
        if(versions.size() == 0){
            return new RepositoryProjectVersionImpl(0, 0, 0, null);
        }
        ProjectVersion max = versions.get(versions.size()-1);
        return max;
    }
    
    public List<ProjectVersion> getVersions() {
        return impl.getVersions();
    }

    public void update(AProjectArtefact artefact) throws ProjectException {
        setProps(artefact.getProps());
        try {
            // clear properties
            for (Property property : getProperties()) {
                removeProperty(property.getName());
            }
            // set all properties
            for (Property property : artefact.getProperties()) {
                addProperty(property);
            }
        } catch (PropertyException e) {
            // TODO log
        }
        setEffectiveDate(artefact.getEffectiveDate());
        setExpirationDate(artefact.getExpirationDate());
        setLineOfBusiness(artefact.getLineOfBusiness());
    }

    public boolean getCanModify() {
        return (getProject().isCheckedOut() && isGranted(PRIVILEGE_EDIT));
    }

    public void refresh() {
        // TODO
    }
}
