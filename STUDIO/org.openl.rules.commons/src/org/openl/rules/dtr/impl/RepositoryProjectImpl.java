package org.openl.rules.dtr.impl;

import org.openl.rules.dtr.RepositoryProject;
import org.openl.rules.dtr.RepositoryProjectArtefact;
import org.openl.rules.dtr.LockInfo;
import org.openl.rules.commons.artefacts.ArtefactPath;
import org.openl.rules.commons.projects.ProjectException;
import org.openl.rules.commons.projects.ProjectVersion;
import org.openl.rules.commons.projects.ProjectDependency;
import org.openl.rules.WorkspaceUser;

import java.util.Collection;
import java.util.LinkedList;

public class RepositoryProjectImpl extends RepositoryProjectFolderImpl implements RepositoryProject {
    private ProjectVersion version;
    private boolean isMarkedForDeletion;
    private LockInfo lock;

    public RepositoryProjectImpl(String name, ArtefactPath path) {
        super(name, path);
    }

    public RepositoryProjectArtefact getArtefactByPath(ArtefactPath artefactPath) throws ProjectException {
        // TODO implement
        throw new ProjectException("Failed to resolve ''{0}''", artefactPath.getStringValue());
    }

    public ProjectVersion getVersion() {
        return version;
    }

    public Collection<ProjectDependency> getDependencies() {
        // TODO -- add dependencies
        return new LinkedList<ProjectDependency>();
    }

    public void lock(WorkspaceUser user) throws ProjectException {
        if (isLocked()) {
            throw new ProjectException("Project ''{0}'' is already locked", getName());
        }

        lock = new LockInfoImpl(new java.util.Date(System.currentTimeMillis()), user.getUserId());
    }

    public void unlock(WorkspaceUser user) throws ProjectException {
        if (!isLocked()) {
            throw new ProjectException("Cannot unlock non-locked project ''{0}''", getName());
        }

        String lockedBy = lock.getLockedBy();
        if (!lockedBy.equals(user.getUserId())) {
            throw new ProjectException("Project ''{0}'' is already locked by ''{0}''", getName(), lockedBy);
        }

        // TODO -- add code
        lock = null;
    }

    public void delete() throws ProjectException {
        if (isMarkedForDeletion) {
            throw new ProjectException("Project ''{0}'' is already marked for deletion", getName());
        }

        //TODO
        isMarkedForDeletion = true;
    }

    public void undelete() throws ProjectException {
        if (!isMarkedForDeletion()) {
            throw new ProjectException("Cannot undelete non-marked project ''{0}''", getName());
        }

        // TODO
        isMarkedForDeletion = false;
    }

    public void erase() throws ProjectException {
        throw new ProjectException("Not enough rights to erase project ''{0}''", getName());

        // TODO
    }

    public boolean isMarkedForDeletion() {
        return isMarkedForDeletion;
    }

    public boolean isLocked() {
        return (lock != null);
    }

    public LockInfo getlLockInfo() {
        return lock;
    }

    // --- protected

}
