package org.openl.rules.project.abstraction;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.openl.rules.common.ArtefactPath;
import org.openl.rules.common.CommonUser;
import org.openl.rules.common.LockInfo;
import org.openl.rules.common.ProjectException;
import org.openl.rules.common.ProjectVersion;
import org.openl.rules.common.PropertiesContainer;
import org.openl.rules.common.Property;
import org.openl.rules.common.PropertyException;
import org.openl.rules.common.RulesRepositoryArtefact;
import org.openl.rules.common.impl.PropertyImpl;
import org.openl.rules.common.impl.RepositoryProjectVersionImpl;
import org.openl.rules.repository.api.ArtefactAPI;
import org.openl.rules.repository.api.ArtefactProperties;

import static org.openl.rules.security.Privileges.*;
import static org.openl.rules.security.AccessManager.isGranted;

public class AProjectArtefact implements PropertiesContainer, RulesRepositoryArtefact {
    private ArtefactAPI impl;
    private AProject project;

    public AProjectArtefact(ArtefactAPI api, AProject project) {
        this.impl = api;
        this.project = project;
    }

    public AProject getProject() {
        return project;
    }

    public ArtefactAPI getAPI() {
        return impl;
    }

    public void setAPI(ArtefactAPI impl) {
        this.impl = impl;
    }

    public Date getEffectiveDate() {
        try {
            return getProperty(ArtefactProperties.PROP_EFFECTIVE_DATE).getDate();
        } catch (PropertyException e) {
            return null;
        }
    }

    public Date getExpirationDate() {
        try {
            return getProperty(ArtefactProperties.PROP_EXPIRATION_DATE).getDate();
        } catch (PropertyException e) {
            return null;
        }
    }

    public String getLineOfBusiness() {
        try {
            return getProperty(ArtefactProperties.PROP_LINE_OF_BUSINESS).getString();
        } catch (PropertyException e) {
            return null;
        }
    }

    public Map<String, Object> getProps() {
        return getAPI().getProps();
    }

    public void setEffectiveDate(Date date) throws PropertyException {
        addProperty(new PropertyImpl(ArtefactProperties.PROP_EFFECTIVE_DATE, date));
    }

    public void setExpirationDate(Date date) throws PropertyException {
        addProperty(new PropertyImpl(ArtefactProperties.PROP_EXPIRATION_DATE, date));
    }

    public void setLineOfBusiness(String lineOfBusiness) throws PropertyException {
        addProperty(new PropertyImpl(ArtefactProperties.PROP_LINE_OF_BUSINESS, lineOfBusiness));
    }

    public void setProps(Map<String, Object> props) throws PropertyException {
        getAPI().setProps(props);
    }

    public void addProperty(Property property) throws PropertyException {
        if (property.getValue() == null) {
            if (hasProperty(property.getName())) {
                removeProperty(property.getName());
            }
        } else {
            getAPI().addProperty(property.getName(), property.getType(), property.getValue());
        }
    }

    public Collection<Property> getProperties() {
        return getAPI().getProperties();
    }

    public Property getProperty(String name) throws PropertyException {
        return getAPI().getProperty(name);
    }

    public boolean hasProperty(String name) {
        return getAPI().hasProperty(name);
    }

    public Property removeProperty(String name) throws PropertyException {
        return getAPI().removeProperty(name);
    }

    public void delete() throws ProjectException {
        getAPI().delete(null);
    }

    public ArtefactPath getArtefactPath() {
        return getAPI().getArtefactPath();
    }

    public String getName() {
        return getAPI().getName();
    }

    public boolean isFolder() {
        return getAPI().isFolder();
    }

    // current version
    public ProjectVersion getVersion() {
        return getAPI().getVersion();
    }

    public ProjectVersion getLastVersion() {
        List<ProjectVersion> versions = getVersions();
        if (versions.size() == 0) {
            return new RepositoryProjectVersionImpl(0, 0, 0, null);
        }
        ProjectVersion max = versions.get(versions.size() - 1);
        return max;
    }

    public List<ProjectVersion> getVersions() {
        return getAPI().getVersions();
    }

    public void update(AProjectArtefact artefact, CommonUser user, int major, int minor) throws ProjectException {
        try {
            setProps(artefact.getProps());
        } catch (PropertyException e1) {
            // TODO log
            e1.printStackTrace();
        }
        try {
            getAPI().removeAllProperties();

            // set all properties
            for (Property property : artefact.getProperties()) {
                addProperty(property);
            }
        } catch (PropertyException e) {
            // TODO log
            e.printStackTrace();
        }
        refresh();
    }

    /**
     * As usual update but this update will use only artefacts which is modified.
     * 
     * @param artefact A source artefact to extract data from.
     * @throws ProjectException
     */
    public void smartUpdate(AProjectArtefact artefact, CommonUser user, int major, int minor) throws ProjectException {
        if (artefact.isModified()) {
            try {
                setProps(artefact.getProps());
            } catch (PropertyException e1) {
                // TODO log
                e1.printStackTrace();
            }
            try {
                getAPI().removeAllProperties();

                // set all properties
                for (Property property : artefact.getProperties()) {
                    addProperty(property);
                }
            } catch (PropertyException e) {
                // TODO log
                e.printStackTrace();
            }
            refresh();
        }
    }

    protected void save(CommonUser user, int major, int minor) throws ProjectException {
        getAPI().commit(user, major, minor, getProject().getVersion().getRevision() + 1);
    }
    
    public boolean getCanModify() {
        return (getProject().isCheckedOut() && isGranted(PRIVILEGE_EDIT));
    }

    public void refresh() {
        // TODO
    }

    public void lock(CommonUser user) throws ProjectException {
        getAPI().lock(user);
    }

    public void unlock(CommonUser user) throws ProjectException {
        getAPI().unlock(user);
    }

    public boolean isLocked() {
        return getLockInfo().isLocked();
    }

    public boolean isLockedByUser(CommonUser user) {
        if (isLocked()) {
            CommonUser lockedBy = getLockInfo().getLockedBy();
            // FIXME
            if (lockedBy.equals(user)) {
                return true;
            }
        }

        return false;
    }

    public LockInfo getLockInfo() {
        return getAPI().getLockInfo();
    }
    
    public boolean isModified(){
        return impl.isModified();
    }
}
