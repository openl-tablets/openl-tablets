package org.openl.rules.project.abstraction;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.openl.rules.common.ArtefactPath;
import org.openl.rules.common.CommonUser;
import org.openl.rules.common.InheritedProperty;
import org.openl.rules.common.LockInfo;
import org.openl.rules.common.ProjectException;
import org.openl.rules.common.ProjectVersion;
import org.openl.rules.common.PropertiesContainer;
import org.openl.rules.common.Property;
import org.openl.rules.common.PropertyException;
import org.openl.rules.common.RulesRepositoryArtefact;
import org.openl.rules.common.impl.RepositoryProjectVersionImpl;
import org.openl.rules.project.impl.local.LocalArtefactAPI;
import org.openl.rules.repository.api.ArtefactAPI;
import org.openl.rules.repository.api.ArtefactProperties;
import org.openl.rules.repository.exceptions.RRepositoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AProjectArtefact implements PropertiesContainer, RulesRepositoryArtefact {
    private final Logger log = LoggerFactory.getLogger(AProjectArtefact.class);

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

    public Map<String, Object> getProps() {
        return getAPI().getProps();
    }

    public Map<String, InheritedProperty> getInheritedProps() {
        return getAPI().getInheritedProps();
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
        int versionsCount = getVersionsCount();
        if (versionsCount == 0) {
            return new RepositoryProjectVersionImpl(0, null);
        }

        try {
            return getVersion(versionsCount - 1);
        } catch (RRepositoryException e) {
            return new RepositoryProjectVersionImpl(0, null);
        }
    }

    public ProjectVersion getFirstVersion() {
        if (getVersionsCount() == 0) {
            return new RepositoryProjectVersionImpl(0, null);
        }

        try {
            return getVersion(1);
        } catch (Exception e) {
            try {
                return getVersion(0);
            } catch (RRepositoryException e1) {
                return new RepositoryProjectVersionImpl(0, null);
            }
        }

    }

    public List<ProjectVersion> getVersions() {
        return getAPI().getVersions();
    }

    public int getVersionsCount() {
        return getAPI().getVersionsCount();
    }

    protected ProjectVersion getVersion(int index) throws RRepositoryException {
        return getAPI().getVersion(index);
    }

    public void update(AProjectArtefact artefact, CommonUser user) throws ProjectException {
        try {
            getAPI().removeAllProperties();

            setProps(artefact.getProps());

            // set all properties
            for (Property property : artefact.getProperties()) {
                addProperty(property);
            }
        } catch (PropertyException e) {
            if (log.isErrorEnabled()) {
                log.error(e.getMessage(), e);
            }
        }

        refresh();
    }

    public void update(AProjectArtefact artefact, CommonUser user, int revision) throws ProjectException {
        try {
            getAPI().removeAllProperties();

            setProps(artefact.getProps());

            // set all properties
            for (Property property : artefact.getProperties()) {
                addProperty(property);
            }
        } catch (PropertyException e) {
            if (log.isErrorEnabled()) {
                log.error(e.getMessage(), e);
            }
        }

        refresh();
    }

    /**
     * As usual update but this update will use only artefacts which is modified.
     * 
     * @param artefact A source artefact to extract data from.
     */
    public void smartUpdate(AProjectArtefact artefact, CommonUser user) throws ProjectException {
        if (artefact.isModified()) {
            try {
                getAPI().removeAllProperties();
                setProps(artefact.getProps());
                
                /*
                for (Property property : artefact.getProperties()) {
                    if(!artefact.getProps().containsKey(property.getName())){
                        addProperty(property);
                    }
                }*/
            } catch (PropertyException e) {
                if (log.isErrorEnabled()) {
                    log.error(e.getMessage(), e);
                }
            }

            refresh();
        }
    }

    protected void commit(CommonUser user) throws ProjectException {
        getAPI().commit(user, getProject().getVersion().getRevision() + 1);
    }

    protected void commit(CommonUser user, int revision) throws ProjectException {
        getAPI().commit(user, revision);
    }

    public void refresh() {
        // TODO
    }

    public void lock(CommonUser user) throws ProjectException {
        getAPI().lock(user);
    }

    public void unlock(CommonUser user) throws ProjectException {
        getAPI().unlock(getUserToUnlock(user));
    }

    public boolean isLocked() {
        return getLockInfo().isLocked();
    }

    public boolean isLockedByUser(CommonUser user) {
        if (isLocked()) {
            CommonUser lockedBy = getLockInfo().getLockedBy();
            if (lockedBy.getUserName().equals(user.getUserName())) {
                return true;
            }

            if (isLockedByDefaultUser(lockedBy, user)) {
                return true;
            }
        }
        return false;
    }

    public LockInfo getLockInfo() {
        return getAPI().getLockInfo();
    }
    
    public boolean isModified(){
        return impl instanceof LocalArtefactAPI && ((LocalArtefactAPI) impl).isModified();
    }

    public void setVersionComment(String versionComment) throws PropertyException {
        //addProperty(new PropertyImpl(ArtefactProperties.VERSION_COMMENT, versionComment));
        getProps().put(ArtefactProperties.VERSION_COMMENT, versionComment);
    }

    public String getVersionComment() {
        /*
        try {
            return getProperty(ArtefactProperties.VERSION_COMMENT).getString();
        } catch (PropertyException e) {
            return null;
        }*/
        if (getProps().containsKey(ArtefactProperties.VERSION_COMMENT)) {
            return getProps().get(ArtefactProperties.VERSION_COMMENT).toString();
        } else {
            return null;
        }
    }

    /**
     * For backward compatibility. Earlier user name in the single user mode analog was "LOCAL".
     * 
     * @param currentUser - current user trying to unlock 
     * @return if lockedUser is LOCAL and current user is DEFAULT then return locked user else return currentUser
     */
    protected CommonUser getUserToUnlock(CommonUser currentUser) {
        if (isLocked()) {
            CommonUser lockedBy = getLockInfo().getLockedBy();
            // For backward compatibility. Earlier user name in single user mode analog was "LOCAL"
            if (isLockedByDefaultUser(lockedBy, currentUser)) {
                currentUser = lockedBy;
            }
        }
        return currentUser;
    }
    
    /**
     * For backward compatibility. Earlier user name in the single user mode analog was "LOCAL".
     * Checks that lockedUser is LOCAL and current user is DEFAULT
     * 
     * @param lockedUser - owner of the lock
     * @param currentUser - current user trying to unlock
     * @return true if owner of the lock is "LOCAL" and current user is "DEFAULT"
     */
    private boolean isLockedByDefaultUser(CommonUser lockedUser, CommonUser currentUser) {
        return "LOCAL".equals(lockedUser.getUserName()) && "DEFAULT".equals(currentUser.getUserName());
    }
}
