package org.openl.rules.workspace.uw.impl;

import org.openl.rules.workspace.WorkspaceUser;
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

    public void setDependencies(Collection<ProjectDependency> dependencies) {
        project.setDependencies(dependencies);
    }

    public void close() throws ProjectException {
        if (!isOpened()) {
            throw new ProjectException("Project ''{0}'' is already closed", null, getName());
        }

        if (isLocked()) {
            dtrProject.unlock(userWorkspace.getUser());
        }
        
        localProject.remove();
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

    public void openVersion(ProjectVersion version) throws ProjectException {
        if (isCheckedOut()) {
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
                    dtrProject.getlLockInfo().getLockedBy(), dtrProject.getlLockInfo().getLockedAt());
        }

        if (isOpened()) {
            close();
        }

        localProject = userWorkspace.openLocalProjectFor(dtrProject);
        dtrProject.lock(userWorkspace.getUser());
        updateArtefact(localProject, dtrProject);
    }

    public void checkIn() throws ProjectException {
        if (!isCheckedOut()) {
            throw new ProjectException("Project ''{0}'' must be checked-out before checking-in", null, getName());
        }

        userWorkspace.checkInProject(localProject);
        // dtrProject != null
        dtrProject.unlock(userWorkspace.getUser());
        updateArtefact(localProject, dtrProject);
    }

    public boolean isCheckedOut() {
        if (isLocalOnly()) {
            return false;
        }

        if (dtrProject.isLocked()) {
            String lockedBy = dtrProject.getlLockInfo().getLockedBy();
            
            if (lockedBy.equals(userWorkspace.getUser().getUserId())) {
                return true;
            }
        }

        return false;
    }

    public boolean isOpened() {
        return (localProject != null);
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
    
    public boolean isLocalOnly() {
        return (dtrProject == null);
    }

    public void delete() throws ProjectException {
        if (isCheckedOut()) {
            throw new ProjectException("Cannot delete project ''{0}'' while it is checked out", null, getName());
        }
        
        if (isOpened()) {
            close();
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
    }
    
    @Override
    protected boolean isLocal() {
        return (project == localProject);
    }
    
    protected boolean isReadOnly() {
        return !isCheckedOut();
    }
    
    protected WorkspaceUser getUser() {
        return userWorkspace.getUser();
    }
}
