package org.openl.rules.workspace.dtr.impl;

import java.util.Collection;
import java.util.LinkedList;

import org.openl.rules.repository.CommonUser;
import org.openl.rules.repository.RProject;
import org.openl.rules.repository.RVersion;
import org.openl.rules.repository.exceptions.RRepositoryException;
import org.openl.rules.workspace.WorkspaceUser;
import org.openl.rules.workspace.abstracts.ArtefactPath;
import org.openl.rules.workspace.abstracts.Project;
import org.openl.rules.workspace.abstracts.ProjectArtefact;
import org.openl.rules.workspace.abstracts.ProjectDependency;
import org.openl.rules.workspace.abstracts.ProjectException;
import org.openl.rules.workspace.abstracts.ProjectVersion;
import org.openl.rules.workspace.dtr.LockInfo;
import org.openl.rules.workspace.dtr.RepositoryProject;
import org.openl.rules.workspace.dtr.RepositoryProjectArtefact;
import org.openl.util.Log;

public class RepositoryProjectImpl extends RepositoryProjectFolderImpl implements RepositoryProject {
    private RProject rulesProject;
    
    protected RepositoryProjectImpl(RProject rulesProject, ArtefactPath path) {
        super(rulesProject, rulesProject.getRootFolder(), path);
        
        this.rulesProject = rulesProject;
    }
    
    public RepositoryProjectArtefact getArtefactByPath(ArtefactPath artefactPath) throws ProjectException {
        // TODO implement
        throw new ProjectException("Failed to resolve ''{0}''", null, artefactPath.getStringValue());
    }

    public ProjectVersion getVersion() {
        RVersion rv = rulesProject.getBaseVersion();
        RepositoryVersionInfoImpl info = new RepositoryVersionInfoImpl(rv.getCreated(), rv.getCreatedBy().getUserName());
        RepositoryProjectVersionImpl version = new RepositoryProjectVersionImpl(rv, info);

        return version;
    }

    public Collection<ProjectDependency> getDependencies() {
        // TODO -- add dependencies
        return new LinkedList<ProjectDependency>();
    }

    public void setDependencies(Collection<ProjectDependency> dependencies) {
        throw new UnsupportedOperationException();
    }

    public void lock(WorkspaceUser user) throws ProjectException {
        if (isLocked()) {
            throw new ProjectException("Project ''{0}'' is already locked", null, getName());
        }

        try {
            rulesProject.lock(user);
        } catch (RRepositoryException e) {
            throw new ProjectException("Cannot lock project: " + e.getMessage(), e);
        }        
    }

    public void unlock(WorkspaceUser user) throws ProjectException {
        if (!isLocked()) {
            throw new ProjectException("Cannot unlock non-locked project ''{0}''", null, getName());
        }

        try {
            rulesProject.unlock(user);
        } catch (RRepositoryException e) {
            throw new ProjectException("Cannot unlock project: " + e.getMessage(), e);
        }        
    }

    public void delete(CommonUser user) throws ProjectException {
        if (isMarkedForDeletion()) {
            throw new ProjectException("Project ''{0}'' is already marked for deletion", null, getName());
        }

//        isMarkedForDeletion = true;
        
        try {
            rulesProject.delete(user);
        } catch (RRepositoryException e) {
            throw new ProjectException("Failed to delete project ''{0}''", e, getName());
        }        
    }

    public void undelete(CommonUser user) throws ProjectException {
        if (!isMarkedForDeletion()) {
            throw new ProjectException("Cannot undelete non-marked project ''{0}''", null, getName());
        }

//        isMarkedForDeletion = false;

        try {
            rulesProject.undelete(user);
        } catch (RRepositoryException e) {
            throw new ProjectException("Failed to undelete project ''{0}''", e, getName());
        }        
    }

    public void erase(CommonUser user) throws ProjectException {
        try {
            rulesProject.erase(user);
        } catch (RRepositoryException e) {
            throw new ProjectException("Failed to erase project ''{0}''", e, getName());
        }        
    }

    public void update(ProjectArtefact srcArtefact) throws ProjectException {
        Project srcProject = (Project) srcArtefact;
        super.update(srcArtefact);
        
        // TODO update dependencies???
        
    }

    public void commit(Project source, CommonUser user) throws ProjectException {
        update(source);

        try {
            rulesProject.commit(user);
        } catch (RRepositoryException e) {
            throw new ProjectException("Failed to commit changes", e);
        }        
    }

    public boolean isMarkedForDeletion() {
        try {
            return rulesProject.isMarked4Deletion();
        } catch (RRepositoryException e) {
            Log.error("isMarkedForDeletion", e);
            return false;
        }
    }

    public boolean isLocked() {
        try {
            return rulesProject.isLocked();
        } catch (RRepositoryException e) {
            Log.error(e);
            return false;
        }        
    }

    public LockInfo getlLockInfo() {
        try {
            return new LockInfoImpl(rulesProject.getLock());
        } catch (RRepositoryException e) {
            Log.error(e);
            return LockInfoImpl.NO_LOCK;
        }        
    }

    public void delete() throws ProjectException {
        throw new ProjectException("Use delete(CommonUser) instead");
    }

        // --- protected

}
