package org.openl.rules.workspace.uw.impl;

import java.util.Collection;

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
import org.openl.util.Log;

public class UserWorkspaceProjectImpl extends UserWorkspaceProjectFolderImpl implements UserWorkspaceProject {
    private Project project;

    private LocalProject localProject;
    private RepositoryProject dtrProject;
    private UserWorkspaceImpl userWorkspace;


    public UserWorkspaceProjectImpl(UserWorkspaceImpl userWorkspace, LocalProject localProject, RepositoryProject dtrProject) {
        super(null, localProject, dtrProject);
        setProject(this);

        this.userWorkspace = userWorkspace;
        updateArtefact(localProject, dtrProject);
    }

    public ProjectArtefact getArtefactByPath(ArtefactPath artefactPath) throws ProjectException {
        return project.getArtefactByPath(artefactPath);
    }

    public ProjectVersion getVersion() {
        return project.getVersion();
    }

    public Collection<ProjectDependency> getDependencies() {
        return project.getDependencies();
    }

    public void setDependencies(Collection<ProjectDependency> dependencies) throws ProjectException {
        if (isReadOnly()) {
            throw new ProjectException("Cannot change dependencies in read only mode", null);
        }
        project.setDependencies(dependencies);
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

    public void open() throws ProjectException {
        if (isLocalOnly()) {
            throw new ProjectException("Project ''{0}'' cannot be opened since it is local only!", null, getName());
        }

        if (isCheckedOut()) {
            throw new ProjectException("Project ''{0}'' is checked-out", null, getName());
        }

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

        localProject = userWorkspace.openLocalProjectFor(dtrProject, version);
        updateArtefact(localProject, dtrProject);
    }

    public void checkOut() throws ProjectException {
        if (isLocalOnly()) {
            throw new ProjectException("Project ''{0}'' cannot be checked out since it is local only!", null, getName());
        }

        if (isCheckedOut()) {
            throw new ProjectException("Project ''{0}'' is already checked-out", null, getName());
        }

        if (isLocked()) {
            throw new ProjectException("Project ''{0}'' is locked by ''{1}'' since ''{2}''", null, getName(),
                    dtrProject.getlLockInfo().getLockedBy().getUserName(), dtrProject.getlLockInfo().getLockedAt());
        }

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

    public void checkIn(int major, int minor) throws ProjectException {
        if (!isCheckedOut()) {
            throw new ProjectException("Project ''{0}'' must be checked-out before checking-in", null, getName());
        }

        userWorkspace.checkInProject(localProject, major, minor);
        // dtrProject != null
        dtrProject.unlock(userWorkspace.getUser());

        // update version, reset & persist states
        localProject.checkedIn(dtrProject.getVersion());

        updateArtefact(localProject, dtrProject);
    }

    public void checkIn() throws ProjectException {
        // do not rise version
        checkIn(0, 0);
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

    public boolean isOpened() {
        return (localProject != null);
    }

    public boolean isOpenedOtherVersion() {
        if (!isOpened()) return false;

        Collection<ProjectVersion> versions = dtrProject.getVersions();

        ProjectVersion max = null;
        for (ProjectVersion version : versions) {
            if (max == null || max.compareTo(version) < 0) {
                max = version;
            }
        }

        return (!getVersion().equals(max));
    }

    public boolean isDeleted() {
        if (isLocalOnly()) {
            return false;
        }

        return dtrProject.isMarkedForDeletion();
    }

    public boolean isLocked() {
        if (dtrProject == null) {
            return false;
        }

        return dtrProject.isLocked();
    }

    public boolean isLockedByMe() {
        if (!isLocked()) return false;

        WorkspaceUser lockedBy = dtrProject.getlLockInfo().getLockedBy();
        return lockedBy.equals(userWorkspace.getUser());
    }

    public boolean isLocalOnly() {
        return (dtrProject == null);
    }

    public void delete() throws ProjectException {
        if (isLocked() && !isLockedByMe()) {
            throw new ProjectException("Cannot delete project ''{0}'' while it is locked by other user", null, getName());
        }

        if (isOpened()) {
            close();
        }

        if (dtrProject != null) {
            dtrProject.delete(userWorkspace.getUser());
        }
    }

    public void undelete() throws ProjectException {
        if (dtrProject != null) {
            dtrProject.undelete(userWorkspace.getUser());
        }
    }

    public void erase() throws ProjectException {
        if (dtrProject != null) {
            dtrProject.erase(userWorkspace.getUser());
        }
    }

    public boolean isDeploymentProject() {
        return false;
    }

    public boolean isRulesProject() {
        return true;
    }

    // --- protected

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
            Log.warn("Project {0} is locked but not opened -- removing lock.", getName());

            try {
                dtrProject.unlock(getUser());
            } catch (ProjectException e) {
                Log.error("Failed to remove lock from project {0}", e, getName());
            }
        }
    }

    @Override
    protected boolean isLocal() {
        return (project == localProject);
    }

    @Override
    public boolean isReadOnly() {
        return !isCheckedOut();
    }

    protected WorkspaceUser getUser() {
        return userWorkspace.getUser();
    }

    public LockInfo getLockInfo() {
        if (dtrProject == null) {
            return null;
        }

        return dtrProject.getlLockInfo();
    }
}
