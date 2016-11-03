package org.openl.rules.project.abstraction;

import org.openl.rules.common.ProjectException;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.workspace.WorkspaceUser;

public abstract class UserWorkspaceProject extends AProject {
    private WorkspaceUser user;

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

    public boolean isOpenedForEditing() {
        return isLockedByMe();
    }

    public boolean isLockedByMe() {
//        return isLockedByUser(user);
        return isOpened();
    }

    public boolean isLocalOnly() {
        return false;
    }

    public abstract boolean isOpened();
    

    /** is opened other version? (not last) */
    public boolean isOpenedOtherVersion() {
        if (!isOpened()) {
            return false;
        }
        return isHistoric();
    }

    public void open() throws ProjectException {
        openVersion(null);
    }

    public abstract void openVersion(String version) throws ProjectException;

    public void save() throws ProjectException {
        save(getUser());
    }

    public void close() throws ProjectException {
        close(user);
    }

    public void delete() throws ProjectException {
        delete(user);
    }

    // TODO Cache status in the field
    public ProjectStatus getStatus() {
        if (isLocalOnly()) {
            return ProjectStatus.LOCAL;
        }
        else if (isDeleted()) {
            return ProjectStatus.ARCHIVED;
        }
        else if (isOpenedForEditing()) {
            return ProjectStatus.EDITING;
        }
        else if (isOpenedOtherVersion()) {
            return ProjectStatus.VIEWING_VERSION;
        }
        else if (isOpened()) {
            return ProjectStatus.VIEWING;
        }
        else {
            return ProjectStatus.CLOSED;
        }
    }

}
