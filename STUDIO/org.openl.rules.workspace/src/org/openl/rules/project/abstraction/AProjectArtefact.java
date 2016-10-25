package org.openl.rules.project.abstraction;

import java.util.*;

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
import org.openl.rules.common.impl.ArtefactPathImpl;
import org.openl.rules.common.impl.RepositoryProjectVersionImpl;
import org.openl.rules.common.impl.RepositoryVersionInfoImpl;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.repository.exceptions.RRepositoryException;
import org.openl.rules.workspace.dtr.impl.LockInfoImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO: Remove PropertiesContainer interface from the class
public class AProjectArtefact implements PropertiesContainer, RulesRepositoryArtefact {
    private final Logger log = LoggerFactory.getLogger(AProjectArtefact.class);

    private AProject project;
    private Repository repository;
    private FileData fileData;
    private String versionComment;

    private Date modifiedTime;

    public AProjectArtefact(AProject project, Repository repository, FileData fileData) {
        this.project = project;
        this.repository = repository;
        this.fileData = fileData;
        this.modifiedTime = fileData == null ? null : fileData.getModifiedAt();
    }

    public AProject getProject() {
        return project;
    }

    public FileData getFileData() {
        return fileData;
    }

    public void setFileData(FileData fileData) {
        this.fileData = fileData;
    }

    public Repository getRepository() {
        return repository;
    }

    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    public Map<String, Object> getProps() {
        return Collections.emptyMap();
    }

    public Map<String, InheritedProperty> getInheritedProps() {
        return Collections.emptyMap();
    }

    public void setProps(Map<String, Object> props) throws PropertyException {
    }

    public void addProperty(Property property) throws PropertyException {
        throw new UnsupportedOperationException();
    }

    public Collection<Property> getProperties() {
        return Collections.emptyList();
    }

    public Property getProperty(String name) throws PropertyException {
        return null;
    }

    public boolean hasProperty(String name) {
        return false;
    }

    public Property removeProperty(String name) throws PropertyException {
        throw new UnsupportedOperationException();
    }

    public void delete() throws ProjectException {
        FileData fileData = getFileData();
        getRepository().delete(fileData.getName());
    }

    public ArtefactPath getArtefactPath() {
        return new ArtefactPathImpl(getFileData().getName());
    }

    public String getName() {
        String name = getFileData().getName();
        return name.substring(name.lastIndexOf("/") + 1);
    }

    public boolean isFolder() {
        return false;
    }

    // current version
    public ProjectVersion getVersion() {
        return createProjectVersion(getFileData());
    }

    public ProjectVersion getLastVersion() {
        List<FileData> fileDatas = getRepository().listHistory(getFileData().getName());
        return fileDatas.isEmpty() ?
               createProjectVersion(null) :
               createProjectVersion(fileDatas.get(fileDatas.size() - 1));
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
        if (getFileData() == null) {
            return Collections.emptyList();
        }
        Collection<FileData> fileDatas = getRepository().listHistory(getFileData().getName());
        List<ProjectVersion> versions = new ArrayList<ProjectVersion>();
        for (FileData data : fileDatas) {
            versions.add(createProjectVersion(data));
        }
        return versions;
    }

    public int getVersionsCount() {
        return getFileData() == null ? 0 : getRepository().listHistory(getFileData().getName()).size();
    }

    protected ProjectVersion getVersion(int index) throws RRepositoryException {
        List<FileData> fileDatas = getRepository().listHistory(getFileData().getName());
        return fileDatas.isEmpty() ? null : createProjectVersion(fileDatas.get(index));
    }

    protected ProjectVersion createProjectVersion(FileData fileData) {
        if (fileData == null) {
            return new RepositoryProjectVersionImpl(0, null);
        }
        RepositoryVersionInfoImpl rvii = new RepositoryVersionInfoImpl(fileData.getModifiedAt(), fileData.getAuthor());
        String version = fileData.getVersion();
        return new RepositoryProjectVersionImpl(version == null ? 0 : Integer.parseInt(version), rvii);
    }

    public void update(AProjectArtefact artefact, CommonUser user) throws ProjectException {
        refresh();
    }

    public void update(AProjectArtefact artefact, CommonUser user, int revision) throws ProjectException {
        refresh();
    }

    public void refresh() {
        // TODO
    }

    public void lock(CommonUser user) throws ProjectException {
        // Do  nothing
    }

    public void unlock(CommonUser user) throws ProjectException {
        // Do  nothing
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
        return LockInfoImpl.NO_LOCK;
    }

    public boolean isModified(){
        FileData fileData = getFileData();
        if (fileData == null) {
            return false;
        }
        if (modifiedTime == null) {
            return true;
        }
        return !modifiedTime.equals(fileData.getModifiedAt());
    }

    public void setVersionComment(String versionComment) throws PropertyException {
        FileData fileData = getFileData();
        if (fileData != null) {
            fileData.setComment(versionComment);
        } else {
            this.versionComment = versionComment;
        }
    }

    public String getVersionComment() {
        FileData fileData = getFileData();
        return fileData == null ? versionComment : fileData.getComment();
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

    public boolean isHistoric() {
        return getFileData().getVersion() != null;
    }

}
