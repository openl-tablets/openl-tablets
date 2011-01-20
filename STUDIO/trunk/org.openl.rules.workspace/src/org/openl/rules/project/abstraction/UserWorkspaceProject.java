package org.openl.rules.project.abstraction;

import static org.openl.rules.security.Privileges.PRIVILEGE_DELETE;
import static org.openl.rules.security.Privileges.PRIVILEGE_DEPLOY;
import static org.openl.rules.security.Privileges.PRIVILEGE_EDIT;
import static org.openl.rules.security.Privileges.PRIVILEGE_ERASE;
import static org.openl.rules.security.Privileges.PRIVILEGE_READ;
import static org.openl.rules.security.AccessManager.isGranted;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import org.openl.rules.common.ArtefactPath;
import org.openl.rules.common.CommonUser;
import org.openl.rules.common.CommonVersion;
import org.openl.rules.common.LockInfo;
import org.openl.rules.common.ProjectException;
import org.openl.rules.common.ProjectVersion;
import org.openl.rules.project.impl.local.LocalFolderAPI;
import org.openl.rules.repository.api.ArtefactAPI;
import org.openl.rules.repository.api.FolderAPI;
import org.openl.rules.workspace.uw.UserWorkspace;

public class UserWorkspaceProject extends AProject {
    private LocalFolderAPI local;
    private FolderAPI repository;
    private UserWorkspace userWorkspace;

    public UserWorkspaceProject(LocalFolderAPI local, FolderAPI repository, UserWorkspace userWorkspace) {
        super(local != null ? local : repository, userWorkspace.getUser());
        this.local = local;
        this.repository = repository;
        this.userWorkspace = userWorkspace;
    }

    public FolderAPI getRepositoryAPI() {
        return repository;
    }

    public LocalFolderAPI getLocalAPI() {
        return local;
    }

    public void checkIn(CommonUser user, int major, int minor) throws ProjectException {
        smartUpdate(local, repository, user, major, minor);
        local.setCurrentVersion(repository.getVersion());
        local.commit(user, 0, 0, 0);// save persistence
        unlock(user);
        refresh();
    }

    @Override
    public void delete() throws ProjectException {
        if (isLocalOnly()) {
            erase();
        } else {
            if (isOpened()) {
                close();
            }
            super.delete();
        }
    }

    public void close() throws ProjectException {
        if (local != null) {
            local.delete(user);
        }
        if (isCheckedOut()) {
            unlock(user);
        }
        if (!isLocalOnly()) {
            setAPI(repository);
        }
        refresh();
    }

    public LockInfo getLockInfo() {
        if (repository != null) {
            return repository.getLockInfo();
        } else {
            return local.getLockInfo();
        }
    }

    @Override
    public void lock(CommonUser user) throws ProjectException {
        repository.lock(user);
    }

    @Override
    public void unlock(CommonUser user) throws ProjectException {
        repository.unlock(user);
    }

    public ProjectVersion getVersion() {
        // TODO ???
        if (isOpened()) {
            return local.getVersion();
        } else {
            return repository.getVersion();
        }
    }

    public List<ProjectVersion> getVersions() {
        if (repository != null) {
            return repository.getVersions();
        } else {
            return local.getVersions();
        }
    }

    public List<ProjectVersion> getVersionsForArtefact(ArtefactPath artefactPath) {
        ArtefactAPI artefact = repository;
        try {
            for (String pathElement : artefactPath.getSegments()) {
                artefact = ((FolderAPI) artefact).getArtefact(pathElement);
            }
            return artefact.getVersions();
        } catch (Exception e) {
            return new LinkedList<ProjectVersion>();
        }
    }

    public boolean isLocalOnly() {
        return repository == null;
    }

    public boolean isOpened() {
        return getAPI() == local;
    }

    public void openVersion(CommonVersion version) throws ProjectException {
        FolderAPI openedProject = repository.getVersion(version);
        File source;
        if (local == null) {
            ArtefactPath path = repository.getArtefactPath();
            source = new File(userWorkspace.getLocalWorkspace().getLocation(), path.segment(path.segmentCount() - 1));
            local = new LocalFolderAPI(source, path, userWorkspace.getLocalWorkspace());
        } else {
            source = local.getSource();
        }
        source.mkdir();
        local.setCurrentVersion(openedProject.getVersion());
        update(openedProject, local, user, version.getMajor(), version.getMinor());
        setAPI(local);
        refresh();
    }

    // FIXME
    private void update(FolderAPI from, FolderAPI to, CommonUser user, int major, int minor) throws ProjectException {
        new AProject(to, user).update(new AProject(from, user), user, major, minor);
    }
    
    private void smartUpdate(FolderAPI from, FolderAPI to, CommonUser user, int major, int minor) throws ProjectException {
        new AProject(to, user).smartUpdate(new AProject(from, user), user, major, minor);
    }
    
    @Override
    public boolean isCheckedOut() {
        // TODO Auto-generated method stub
        return super.isCheckedOut();
    }
    
    public boolean getCanCheckOut() {
        if (isLocalOnly() || isCheckedOut() || isLocked()) {
            return false;
        }

        return isGranted(PRIVILEGE_EDIT);
    }

    public boolean getCanClose() {
        return (!isLocalOnly() && isOpened());
    }

    public boolean getCanDelete() {
        if (isLocalOnly()) {
            // any user can delete own local project
            return true;
        }

        return (!isLocked() || isLockedByMe()) && isGranted(PRIVILEGE_DELETE);
    }

    public boolean getCanErase() {
        return (isDeleted() && isGranted(PRIVILEGE_ERASE));
    }

    public boolean getCanExport() {
        return getCanOpen();
    }

    public boolean getCanOpen() {
        if (isLocalOnly() || isCheckedOut()) {
            return false;
        }

        return isGranted(PRIVILEGE_READ);
    }

    public boolean getCanCompare() {
        if (isLocalOnly()) {
            return false;
        }
        return isGranted(PRIVILEGE_READ);
    }

    public boolean getCanRedeploy() {
        if (isLocalOnly() || isCheckedOut()) {
            return false;
        }

        return isGranted(PRIVILEGE_DEPLOY);
    }

    public boolean getCanUndelete() {
        return (isDeleted() && isGranted(PRIVILEGE_EDIT));
    }
}
