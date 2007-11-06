package org.openl.rules.workspace.uw.impl;

import org.openl.rules.workspace.abstracts.ArtefactPath;
import org.openl.rules.workspace.abstracts.Project;
import org.openl.rules.workspace.abstracts.ProjectArtefact;
import org.openl.rules.workspace.abstracts.ProjectDependency;
import org.openl.rules.workspace.abstracts.ProjectException;
import org.openl.rules.workspace.abstracts.ProjectVersion;
import org.openl.rules.workspace.dtr.RepositoryProject;
import org.openl.rules.workspace.lw.LocalProject;
import org.openl.rules.workspace.uw.UserWorkspaceProject;

import java.util.Collection;

public class UserWorkspaceProjectImpl extends UserWorkspaceProjectFolderImpl implements UserWorkspaceProject {
    private Project project;

    private LocalProject localProject;
    private RepositoryProject dtrProject;
    private UserWorkspaceImpl userWorkspace;


    public UserWorkspaceProjectImpl(UserWorkspaceImpl userWorkspace, LocalProject localProject, RepositoryProject dtrProject) {
        super(null, localProject, dtrProject);
        setProject(this);
        
        this.userWorkspace = userWorkspace;
        this.localProject = localProject;
        this.dtrProject = dtrProject;

        updateActiveProject();
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

    public void close() throws ProjectException {
        if (!isOpened()) {
            throw new ProjectException("Project ''{0}'' is already closed", null, getName());
        }

        localProject.remove();
        localProject = null;
        updateActiveProject();
    }

    public void open() throws ProjectException {
        if (isCheckedOut()) {
            throw new ProjectException("Project ''{0}'' is checked-out", null, getName());
        }

        if (isOpened()) {
            close();
        }

        localProject = userWorkspace.openLocalProjectFor(dtrProject);

        updateActiveProject();
    }

    public void openVersion(ProjectVersion version) throws ProjectException {
        if (isCheckedOut()) {
            close();
        }

        localProject = userWorkspace.openLocalProjectFor(dtrProject, version);

        updateActiveProject();
    }

    public void checkOut() throws ProjectException {
        if (!isCheckedOut()) {
            throw new ProjectException("Project ''{0}'' is already checked-out", null, getName());
        }

        if (isLocked()) {
            throw new ProjectException("Project ''{0}'' is locked by ''{1}'' since ''{2}''", null, getName(),
                    dtrProject.getlLockInfo().getLockedBy(), dtrProject.getlLockInfo().getLockedAt());
        }

        if (isOpened()) {
            close();
        }

        localProject = userWorkspace.openLocalProjectFor(dtrProject);
        dtrProject.lock(userWorkspace.getUser());

        updateActiveProject();
    }

    public void checkIn() throws ProjectException {
        if (!isCheckedOut()) {
            throw new ProjectException("Project ''{0}'' must be checked-out before checking-in", null, getName());
        }

        userWorkspace.checkInProject(localProject);
        dtrProject.unlock(userWorkspace.getUser());

        updateActiveProject();
    }

    public boolean isCheckedOut() {
        String lockedBy = dtrProject.getlLockInfo().getLockedBy();
        if (lockedBy.equals("127.0.0.1")) {
            return true;
        }

        return false;
    }

    public boolean isOpened() {
        return (localProject != null);
    }

    public boolean isDeleted() {
        return dtrProject.isMarkedForDeletion();
    }

    public boolean isLocked() {
        return dtrProject.isLocked();
    }

    // --- protected

    @Override
    protected boolean isLocal() {
        return (project == localProject);
    }
    
    protected boolean isReadOnly() {
        return !isCheckedOut();
    }

    protected void updateActiveProject() {
        if (localProject == null) {
            project = dtrProject;
        } else {
            project = localProject;
        }
    }
}
