package org.openl.rules.workspace.uw.impl;

import org.openl.rules.workspace.abstracts.ArtefactPath;
import org.openl.rules.workspace.abstracts.Project;
import org.openl.rules.workspace.abstracts.ProjectArtefact;
import org.openl.rules.workspace.abstracts.ProjectException;
import org.openl.rules.workspace.abstracts.ProjectVersion;
import org.openl.rules.workspace.dtr.RepositoryProject;
import org.openl.rules.workspace.lw.LocalProject;
import org.openl.rules.workspace.props.Property;
import org.openl.rules.workspace.props.PropertyException;
import org.openl.rules.workspace.props.PropertyTypeException;
import org.openl.rules.workspace.uw.UserWorkspaceProject;

import java.util.Collection;

public class UserWorkspaceProjectImpl implements UserWorkspaceProject {
    private Project project;

    private LocalProject localProject;
    private RepositoryProject dtrProject;
    private UserWorkspaceImpl userWorkspace;


    public UserWorkspaceProjectImpl(UserWorkspaceImpl userWorkspace, LocalProject localProject, RepositoryProject dtrProject) {
        this.userWorkspace = userWorkspace;
        this.localProject = localProject;
        this.dtrProject = dtrProject;

        updateActiveProject();
    }

    public boolean hasProperty(String name) {
        return project.hasProperty(name);
    }

    public Property getProperty(String name) throws PropertyException {
        return project.getProperty(name);
    }

    public Collection<Property> getProperties() {
        return project.getProperties();
    }

    public void addProperty(Property property) throws PropertyTypeException {
        project.addProperty(property);
    }

    public Property removeProperty(String name) throws PropertyException {
        return project.removeProperty(name);
    }

    public String getName() {
        return project.getName();
    }

    public ArtefactPath getArtefactPath() {
        return project.getArtefactPath();
    }

    public ProjectArtefact getArtefact(String name) throws ProjectException {
        return project.getArtefact(name);
    }

    public Collection getArtefacts() {
        return project.getArtefacts();
    }

    public ProjectArtefact getArtefactByPath(ArtefactPath artefactPath) throws ProjectException {
        return project.getArtefactByPath(artefactPath);
    }

    public ProjectVersion getVersion() {
        return project.getVersion();
    }

    public Collection getDependencies() {
        return project.getDependencies();
    }

    public void close() throws ProjectException {
        if (!isOpened()) {
            throw new ProjectException("Project ''{0}'' is already closed", getName());
        }

        localProject.remove();
        localProject = null;
        updateActiveProject();
    }

    public void open() throws ProjectException {
        if (isCheckedOut()) {
            throw new ProjectException("Project ''{0}'' is checked-out", getName());
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
            throw new ProjectException("Project ''{0}'' is already checked-out", getName());
        }

        if (isLocked()) {
            throw new ProjectException("Project ''{0}'' is locked by ''{1}'' since ''{2}''", getName(),
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
            throw new ProjectException("Project ''{0}'' must be checked-out before checking-in", getName());
        }

        userWorkspace.checkInProject(localProject);
        dtrProject.unlock(userWorkspace.getUser());

        updateActiveProject();
    }

    public Collection<ProjectVersion> getVersions() {
        return dtrProject.getVersions();
    }

    public boolean isCheckedOut() {
        String lockedBy = dtrProject.getlLockInfo().getLockedBy();
        if (lockedBy.equals(null)) {
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

    protected void updateActiveProject() {
        if (localProject == null) {
            project = dtrProject;
        } else {
            project = localProject;
        }
    }
}
