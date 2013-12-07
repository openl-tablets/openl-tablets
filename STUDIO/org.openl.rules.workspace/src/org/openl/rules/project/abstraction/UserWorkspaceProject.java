package org.openl.rules.project.abstraction;

import org.openl.rules.common.CommonVersion;
import org.openl.rules.common.ProjectException;
import org.openl.rules.common.ProjectVersion;
import org.openl.rules.repository.api.FolderAPI;
import org.openl.rules.workspace.WorkspaceUser;

public abstract class UserWorkspaceProject extends AProject {
    private WorkspaceUser user;

    public UserWorkspaceProject(FolderAPI api, WorkspaceUser user) {
        super(api);
        this.user = user;
    }

    protected WorkspaceUser getUser() {
        return user;
    }

    public boolean isOpenedForEditing() {
        return isLockedByMe();
    }

    public boolean isLockedByMe() {
        return isLockedByUser(user);
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
        ProjectVersion max = getLastVersion();
        if (max == null) {
            return false;
        }
        return (!getVersion().equals(max));
    }

    public void open() throws ProjectException {
        openVersion(getLastVersion());
    }

    public abstract void openVersion(CommonVersion version) throws ProjectException;

    public void edit() throws ProjectException {
        edit(getUser());
    }
    
    public void save() throws ProjectException {
        save(getUser());
    }

    public void close() throws ProjectException {
        close(user);
    }

    public void delete() throws ProjectException {
        delete(user);
    }

    public void erase() throws ProjectException {
        erase(user);
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
