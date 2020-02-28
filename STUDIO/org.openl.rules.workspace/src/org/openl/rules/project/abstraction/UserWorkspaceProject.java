package org.openl.rules.project.abstraction;

import java.io.IOException;

import org.openl.rules.common.CommonUser;
import org.openl.rules.common.LockInfo;
import org.openl.rules.common.ProjectException;
import org.openl.rules.repository.api.BranchRepository;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.workspace.WorkspaceUser;

public abstract class UserWorkspaceProject extends AProject {
    private final WorkspaceUser user;

    public UserWorkspaceProject(WorkspaceUser user, Repository repository, String folderPath, String version) {
        super(repository, folderPath, version);
        this.user = user;
    }

    public UserWorkspaceProject(WorkspaceUser user, Repository repository, FileData fileData) {
        super(repository, fileData);
        this.user = user;
    }

    protected WorkspaceUser getUser() {
        return user;
    }

    @Override
    public boolean isOpenedForEditing() {
        return isOpened() && (!isLocked() || isLockedByMe());
    }

    public boolean isLockedByMe() {
        return isLockedByUser(user);
    }

    public boolean isLockedByMe(LockInfo lockInfo) {
        return isLockedByUser(lockInfo, user);
    }

    /**
     * Unlocks the project only if it's locked by current user. Otherwise do nothing.
     */
    public void releaseMyLock() throws ProjectException {
        if (isLockedByMe()) {
            unlock();
        }
    }

    public boolean isLocalOnly() {
        return false;
    }

    public abstract boolean isOpened();

    /** is opened other version? (not last) */
    public boolean isOpenedOtherVersion() {
        return isOpened() && !isLastVersion();
    }

    public void open() throws ProjectException {
        openVersion(null);
    }

    public abstract void openVersion(String version) throws ProjectException;

    public void save() throws ProjectException {
        save(getUser());
    }

    public abstract void save(CommonUser user) throws ProjectException;

    public void close() throws ProjectException {
        close(user);
    }

    public void delete(String comment) throws ProjectException {
        delete(user, comment);
    }

    @Override
    public void refresh() {
        super.refresh();
        setFileData(null);
    }

    // TODO Cache status in the field
    public ProjectStatus getStatus() {
        if (isLocalOnly()) {
            return ProjectStatus.LOCAL;
        } else if (isDeleted()) {
            return ProjectStatus.ARCHIVED;
        } else if (isModified()) {
            return ProjectStatus.EDITING;
        } else if (isOpenedOtherVersion()) {
            return ProjectStatus.VIEWING_VERSION;
        } else if (isOpened()) {
            if (isLockedByMe()) {
                return ProjectStatus.VIEWING;
            } else {
                return ProjectStatus.VIEWING_VERSION;
            }

        } else {
            return ProjectStatus.CLOSED;
        }
    }

    protected void setDesignRepository(Repository repository) {
        setRepository(repository);
    }

    public Repository getDesignRepository() {
        return getRepository();
    }

    public boolean isSupportsBranches() {
        return !isLocalOnly() && getDesignRepository().supports().branches();
    }

    public void setBranch(String newBranch) throws ProjectException {
        BranchRepository branchRepository = (BranchRepository) getDesignRepository();
        String currentBranch = branchRepository.getBranch();
        if (!newBranch.equals(currentBranch)) {
            try {
                setDesignRepository(branchRepository.forBranch(newBranch));
            } catch (IOException e) {
                throw new ProjectException(e.getMessage(), e);
            }
            setHistoryVersion(null);
            refresh();
            getFileData(); // Reinitialize file data
        }
    }

    public String getBranch() {
        if (isSupportsBranches()) {
            return ((BranchRepository) getDesignRepository()).getBranch();
        }

        return null;
    }
}
