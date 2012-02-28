package org.openl.rules.project.abstraction;

import org.openl.rules.common.CommonUser;
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
    
    public boolean isCheckedOut() {
        return isLockedByMe();
    }

    public boolean isLockedByMe() {
        if (isLocked()) {
            CommonUser lockedBy = getLockInfo().getLockedBy();
            if (lockedBy.getUserName().equals(user.getUserName())) {
                return true;
            }
        }

        return false;
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

    public void checkOut() throws ProjectException {
        checkOut(getUser());
    }
    
    public void checkIn() throws ProjectException {
        checkIn(getUser());
    }
    
    public void checkIn(int major, int minor) throws ProjectException {
        checkIn(getUser(), major, minor);
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

}
