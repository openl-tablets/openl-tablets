package org.openl.rules.workspace.uw.impl;

import java.io.File;
import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.rules.repository.CommonVersion;
import org.openl.rules.workspace.WorkspaceUser;
import org.openl.rules.workspace.abstracts.ArtefactPath;
import org.openl.rules.workspace.abstracts.Project;
import org.openl.rules.workspace.abstracts.ProjectArtefact;
import org.openl.rules.workspace.abstracts.ProjectDependency;
import org.openl.rules.workspace.abstracts.ProjectException;
import org.openl.rules.workspace.abstracts.ProjectVersion;
import org.openl.rules.workspace.dtr.LockInfo;
import org.openl.rules.workspace.dtr.RepositoryProject;
import org.openl.rules.workspace.lw.LocalProject;
import org.openl.rules.workspace.uw.UserWorkspaceProject;
import org.openl.util.MsgHelper;
import static org.openl.rules.security.Privileges.*;
import static org.openl.rules.security.SecurityUtils.check;
import static org.openl.rules.security.SecurityUtils.isGranted;

public class UserWorkspaceProjectImpl extends UserWorkspaceProjectFolderImpl implements UserWorkspaceProject {
    private static final Log log = LogFactory.getLog(UserWorkspaceProjectImpl.class);

    private Project project;

    private LocalProject localProject;
    private RepositoryProject dtrProject;
    private UserWorkspaceImpl userWorkspace;

    public UserWorkspaceProjectImpl(UserWorkspaceImpl userWorkspace, LocalProject localProject,
            RepositoryProject dtrProject) {
        super(null, localProject, dtrProject);
        setProject(this);

        this.userWorkspace = userWorkspace;
        updateArtefact(localProject, dtrProject);
    }

    public void checkIn() throws ProjectException {
        // do not rise version
        checkIn(0, 0);
    }

    public void checkIn(int major, int minor) throws ProjectException {
        if (!isCheckedOut()) {
            throw new ProjectException("Project ''{0}'' must be checked-out before checking-in!", null, getName());
        }

        userWorkspace.checkInProject(localProject, major, minor);
        // dtrProject != null
        dtrProject.unlock(userWorkspace.getUser());

        // update version, reset & persist states
        localProject.checkedIn(dtrProject.getVersion());

        dtrProject.update(localProject);
        updateArtefact(localProject, dtrProject);
    }

    public void checkOut() throws ProjectException {
        if (isLocalOnly()) {
            throw new ProjectException("Project ''{0}'' cannot be checked out since it is local only!", null, getName());
        }

        if (isCheckedOut()) {
            throw new ProjectException("Project ''{0}'' is already checked-out!", null, getName());
        }

        if (isLocked()) {
            throw new ProjectException("Project ''{0}'' is locked by ''{1}'' since ''{2}''!", null, getName(),
                    dtrProject.getlLockInfo().getLockedBy().getUserName(), dtrProject.getlLockInfo().getLockedAt());
        }

        check(PRIVILEGE_EDIT);

        if (isOpened()) {
            close();
        }

        localProject = userWorkspace.openLocalProjectFor(dtrProject);
        dtrProject.lock(userWorkspace.getUser());
        updateArtefact(localProject, dtrProject);
    }

    protected void checkOutLocal() throws ProjectException {
        dtrProject.lock(userWorkspace.getUser());
    }

    public void close() throws ProjectException {
        if (isLockedByMe()) {
            dtrProject.unlock(userWorkspace.getUser());
        }

        if (localProject != null) {
            localProject.remove();
        }

        updateArtefact(null, dtrProject);
    }

    @Override
    public void delete() throws ProjectException {
        if (isLocked() && !isLockedByMe()) {
            throw new ProjectException("Cannot delete project ''{0}'' while it is locked by other user!", null,
                    getName());
        }

        check(PRIVILEGE_DELETE);

        if (isOpened()) {
            close();
        }

        if (dtrProject != null) {
            dtrProject.delete(userWorkspace.getUser());
        }
    }

    public void erase() throws ProjectException {
        check(PRIVILEGE_ERASE);

        if (dtrProject != null) {
            dtrProject.erase(userWorkspace.getUser());
        }
    }

    public File exportVersion(CommonVersion version) throws ProjectException {
        check(PRIVILEGE_READ);

        return userWorkspace.exportProject(dtrProject, version);
    }

    public ProjectArtefact getArtefactByPath(ArtefactPath artefactPath) throws ProjectException {
        return project.getArtefactByPath(artefactPath);
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

    public Collection<ProjectDependency> getDependencies() {
        return project.getDependencies();
    }

    public LockInfo getLockInfo() {
        if (dtrProject == null) {
            return null;
        }

        return dtrProject.getlLockInfo();
    }

    protected WorkspaceUser getUser() {
        return userWorkspace.getUser();
    }

    public ProjectVersion getVersion() {
        return project.getVersion();
    }

    public boolean isCheckedOut() {
        if (isLocalOnly()) {
            return false;
        }

        if (dtrProject.isLocked()) {
            WorkspaceUser lockedBy = dtrProject.getlLockInfo().getLockedBy();

            if (lockedBy.equals(userWorkspace.getUser())) {
                return true;
            }
        }

        return false;
    }

    public boolean isDeleted() {
        if (isLocalOnly()) {
            return false;
        }

        return dtrProject.isMarkedForDeletion();
    }

    public boolean isDeploymentProject() {
        return false;
    }

    @Override
    protected boolean isLocal() {
        return (project == localProject);
    }

    public boolean isLocalOnly() {
        return (dtrProject == null);
    }

    public boolean isLocked() {
        if (dtrProject == null) {
            return false;
        }

        return dtrProject.isLocked();
    }

    public boolean isLockedByMe() {
        if (!isLocked()) {
            return false;
        }

        WorkspaceUser lockedBy = dtrProject.getlLockInfo().getLockedBy();
        return lockedBy.equals(userWorkspace.getUser());
    }

    public boolean isOpened() {
        return (localProject != null);
    }

    // --- protected

    public boolean isOpenedOtherVersion() {
        if (!isOpened()) {
            return false;
        }

        Collection<ProjectVersion> versions = dtrProject.getVersions();

        ProjectVersion max = null;
        for (ProjectVersion version : versions) {
            if (max == null || max.compareTo(version) < 0) {
                max = version;
            }
        }

        return (!getVersion().equals(max));
    }

    @Override
    public boolean isReadOnly() {
        return !isCheckedOut();
    }

    public boolean isRulesProject() {
        return true;
    }

    public void open() throws ProjectException {
        if (isLocalOnly()) {
            throw new ProjectException("Project ''{0}'' cannot be opened since it is local only!", null, getName());
        }

        if (isCheckedOut()) {
            throw new ProjectException("Project ''{0}'' is checked-out!", null, getName());
        }

        check(PRIVILEGE_READ);

        if (isOpened()) {
            close();
        }

        localProject = userWorkspace.openLocalProjectFor(dtrProject);
        updateArtefact(localProject, dtrProject);
    }

    public void openVersion(CommonVersion version) throws ProjectException {
        if (isCheckedOut() || isOpened()) {
            close();
        }

        check(PRIVILEGE_READ);

        localProject = userWorkspace.openLocalProjectFor(dtrProject, version);
        updateArtefact(localProject, dtrProject);
    }

    public void setDependencies(Collection<ProjectDependency> dependencies) throws ProjectException {
        if (isReadOnly()) {
            throw new ProjectException("Cannot change dependencies in read only mode!", null);
        }
        project.setDependencies(dependencies);
    }

    public void undelete() throws ProjectException {
        check(PRIVILEGE_EDIT);

        if (dtrProject != null) {
            dtrProject.undelete(userWorkspace.getUser());
        }
    }

    protected void updateArtefact(LocalProject localProject, RepositoryProject dtrProject) {
        super.updateArtefact(localProject, dtrProject);

        this.localProject = localProject;
        this.dtrProject = dtrProject;

        if (localProject == null) {
            project = dtrProject;
        } else {
            project = localProject;
        }

        if (isLockedByMe() && isOpened() == false) {
            // locked but no in local workspace
            String msg = MsgHelper.format("Project ''{0}'' is locked but not opened -- removing lock.", getName());
            log.warn(msg);

            try {
                dtrProject.unlock(getUser());
            } catch (ProjectException e) {
                msg = MsgHelper.format("Failed to remove lock from project ''{0}''!", getName());
                log.error(msg, e);
            }
        }
    }
}
