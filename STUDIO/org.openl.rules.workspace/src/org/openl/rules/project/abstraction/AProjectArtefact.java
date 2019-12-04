package org.openl.rules.project.abstraction;

import java.io.IOException;
import java.util.*;

import org.openl.rules.common.*;
import org.openl.rules.common.impl.ArtefactPathImpl;
import org.openl.rules.common.impl.RepositoryProjectVersionImpl;
import org.openl.rules.common.impl.RepositoryVersionInfoImpl;
import org.openl.rules.project.impl.local.SimpleLockInfo;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.Repository;
import org.openl.util.RuntimeExceptionWrapper;

public class AProjectArtefact {
    private AProject project;
    private Repository repository;
    private FileData fileData;

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

    public void delete() throws ProjectException {
        FileData fileData = getFileData();
        try {
            getRepository().delete(fileData);
        } catch (IOException e) {
            throw new ProjectException(e.getMessage(), e);
        }
    }

    public ArtefactPath getArtefactPath() {
        return new ArtefactPathImpl(getFileData().getName());
    }

    public String getInternalPath() {
        String projectPath = getProject().getFileData().getName();
        return getFileData().getName().substring(projectPath.length() + 1);
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
        List<FileData> fileDatas;
        try {
            fileDatas = getRepository().listHistory(getFileData().getName());
        } catch (IOException ex) {
            throw RuntimeExceptionWrapper.wrap(ex);
        }
        return fileDatas.isEmpty() ? createProjectVersion(null)
                                   : createProjectVersion(fileDatas.get(fileDatas.size() - 1));
    }

    public ProjectVersion getFirstVersion() {
        try {
            int versionsCount = getVersionsCount();
            if (versionsCount == 0) {
                return new RepositoryProjectVersionImpl();
            }

            return getVersion(0);
        } catch (Exception e) {
            return new RepositoryProjectVersionImpl();
        }
    }

    public List<ProjectVersion> getVersions() {
        if (getFileData() == null) {
            return Collections.emptyList();
        }
        Collection<FileData> fileDatas;
        try {
            fileDatas = getRepository().listHistory(getFileData().getName());
        } catch (IOException ex) {
            throw RuntimeExceptionWrapper.wrap(ex);
        }
        List<ProjectVersion> versions = new ArrayList<>();
        for (FileData data : fileDatas) {
            versions.add(createProjectVersion(data));
        }
        return versions;
    }

    public boolean hasModifications() {
        return !getFirstVersion().equals(getLastVersion());
    }

    public int getVersionsCount() {
        try {
            return getFileData() == null ? 0 : getRepository().listHistory(getFileData().getName()).size();
        } catch (IOException ex) {
            throw RuntimeExceptionWrapper.wrap(ex);
        }
    }

    protected ProjectVersion getVersion(int index) {
        List<FileData> fileDatas;
        try {
            fileDatas = getRepository().listHistory(getFileData().getName());
        } catch (IOException ex) {
            throw RuntimeExceptionWrapper.wrap(ex);
        }
        return fileDatas.isEmpty() ? null : createProjectVersion(fileDatas.get(index));
    }

    protected ProjectVersion createProjectVersion(FileData fileData) {
        if (fileData == null) {
            return new RepositoryProjectVersionImpl();
        }
        RepositoryVersionInfoImpl rvii = new RepositoryVersionInfoImpl(fileData.getModifiedAt(), fileData.getAuthor());
        String version = fileData.getVersion();
        RepositoryProjectVersionImpl projectVersion = new RepositoryProjectVersionImpl(version == null ? "0" : version,
            rvii,
            fileData.isDeleted());
        projectVersion.setVersionComment(fileData.getComment());
        return projectVersion;
    }

    public void update(AProjectArtefact artefact, CommonUser user) throws ProjectException {
        refresh();
    }

    public void refresh() {
    }

    public void lock() throws ProjectException {
        // Do nothing
    }

    public void unlock() throws ProjectException {
        // Do nothing
    }

    public boolean isLocked() {
        return getLockInfo().isLocked();
    }

    public boolean isLockedByUser(CommonUser user) {
        return isLockedByUser(getLockInfo(), user);
    }

    protected boolean isLockedByUser(LockInfo lockInfo, CommonUser user) {
        if (lockInfo.isLocked()) {
            CommonUser lockedBy = lockInfo.getLockedBy();
            return lockedBy.getUserName().equals(user.getUserName()) || isLockedByDefaultUser(lockedBy, user);

        }
        return false;
    }

    public LockInfo getLockInfo() {
        return SimpleLockInfo.NO_LOCK;
    }

    public boolean isModified() {
        FileData fileData = getFileData();
        return fileData != null && (modifiedTime == null || !modifiedTime.equals(fileData.getModifiedAt()));
    }

    /**
     * For backward compatibility. Earlier user name in the single user mode analog was "LOCAL". Checks that lockedUser
     * is LOCAL and current user is DEFAULT
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
