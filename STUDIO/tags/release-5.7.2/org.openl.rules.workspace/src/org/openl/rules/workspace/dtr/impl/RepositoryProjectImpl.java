package org.openl.rules.workspace.dtr.impl;

import java.util.Collection;
import java.util.LinkedList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.rules.repository.CommonUser;
import org.openl.rules.repository.CommonVersion;
import org.openl.rules.repository.RDependency;
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
import org.openl.rules.workspace.abstracts.impl.ProjectDependencyImpl;
import org.openl.rules.workspace.dtr.LockInfo;
import org.openl.rules.workspace.dtr.RepositoryProject;
import org.openl.rules.workspace.dtr.RepositoryProjectArtefact;
import org.openl.util.MsgHelper;

public class RepositoryProjectImpl extends RepositoryProjectFolderImpl implements RepositoryProject {
    private static final Log log = LogFactory.getLog(RepositoryProjectImpl.class);

    private RProject rulesProject;

    protected RepositoryProjectImpl(RProject rulesProject, ArtefactPath path) {
        super(rulesProject, rulesProject.getRootFolder(), path);

        this.rulesProject = rulesProject;
    }

    public void commit(Project source, CommonUser user) throws ProjectException {
        if (log.isDebugEnabled()) {
            String msg = MsgHelper.format("Updating project ''{0}''...", getName());
            log.debug(msg);
        }
        update(source);

        try {
            String msg = MsgHelper.format("Committing project ''{0}'' by user ''{1}''...", getName(), user
                    .getUserName());
            log.debug(msg);
            rulesProject.commit(user);
        } catch (RRepositoryException e) {
            throw new ProjectException("Failed to commit changes!", e);
        }
    }

    @Override
    public void delete() throws ProjectException {
        throw new ProjectException("Use delete(CommonUser) instead!");
    }

    public void delete(CommonUser user) throws ProjectException {
        if (isMarkedForDeletion()) {
            throw new ProjectException("Project ''{0}'' is already marked for deletion!", null, getName());
        }

        // isMarkedForDeletion = true;

        try {
            rulesProject.delete(user);
        } catch (RRepositoryException e) {
            throw new ProjectException("Failed to delete project ''{0}''!", e, getName());
        }
    }

    public void erase(CommonUser user) throws ProjectException {
        try {
            rulesProject.erase(user);
        } catch (RRepositoryException e) {
            throw new ProjectException("Failed to erase project ''{0}''!", e, getName());
        }
    }

    public RepositoryProjectArtefact getArtefactByPath(ArtefactPath artefactPath) throws ProjectException {
        RepositoryProjectArtefact a = this;
        for (String s : artefactPath.getSegments()) {
            a = a.getArtefact(s);
        }
        return a;
    }

    public Collection<ProjectDependency> getDependencies() {
        LinkedList<ProjectDependency> result = new LinkedList<ProjectDependency>();

        try {
            for (RDependency rDep : rulesProject.getDependencies()) {
                String projectName = rDep.getProjectName();

                ProjectVersion lowVer = new RepositoryProjectVersionImpl(rDep.getLowerLimit(), null);

                ProjectVersion upVer = null;
                CommonVersion dependencyUpperLimit = rDep.getUpperLimit();
                if (dependencyUpperLimit != null) {
                    upVer = new RepositoryProjectVersionImpl(dependencyUpperLimit, null);
                }

                ProjectDependency pd = new ProjectDependencyImpl(projectName, lowVer, upVer);

                result.add(pd);
            }
        } catch (RRepositoryException e) {
            log.error("Cannot get dependencies!", e);
        }

        return result;
    }

    public LockInfo getlLockInfo() {
        try {
            return new LockInfoImpl(rulesProject.getLock());
        } catch (RRepositoryException e) {
            log.error("getLockInfo", e);
            return LockInfoImpl.NO_LOCK;
        }
    }

    public ProjectVersion getVersion() {
        RVersion rv = rulesProject.getActiveVersion();

        if (rv == null) {
            return null;
        }

        RepositoryVersionInfoImpl info = new RepositoryVersionInfoImpl(rv.getCreated(), rv.getCreatedBy().getUserName());
        RepositoryProjectVersionImpl version = new RepositoryProjectVersionImpl(rv, info);

        return version;
    }

    public boolean isLocked() {
        try {
            return rulesProject.isLocked();
        } catch (RRepositoryException e) {
            log.error("isLocked", e);
            return false;
        }
    }

    public boolean isMarkedForDeletion() {
        try {
            return rulesProject.isMarked4Deletion();
        } catch (RRepositoryException e) {
            log.debug("isMarkedForDeletion", e);
            // most likely project was erased
            return true;
        }
    }

    public void lock(WorkspaceUser user) throws ProjectException {
        if (isLocked()) {
            throw new ProjectException("Project ''{0}'' is already locked!", null, getName());
        }

        try {
            rulesProject.lock(user);
        } catch (RRepositoryException e) {
            throw new ProjectException("Cannot lock project ''{0}'': " + e.getMessage(), e, getName());
        }
    }

    public void riseVersion(int major, int minor) throws ProjectException {
        try {
            rulesProject.riseVersion(major, minor);
        } catch (RRepositoryException e) {
            throw new ProjectException(e.getMessage(), e);
        }
    }

    public void setDependencies(Collection<ProjectDependency> dependencies) throws ProjectException {
        try {
            LinkedList<RDependency> newDeps = new LinkedList<RDependency>();
            for (ProjectDependency pd : dependencies) {
                newDeps.add(pd);
            }

            rulesProject.setDependencies(newDeps);
        } catch (RRepositoryException e) {
            throw new ProjectException("Cannot update dependencies!", e);
        }
    }

    public void undelete(CommonUser user) throws ProjectException {
        if (!isMarkedForDeletion()) {
            throw new ProjectException("Cannot undelete non-marked project ''{0}''!", null, getName());
        }

        // isMarkedForDeletion = false;

        try {
            rulesProject.undelete(user);
        } catch (RRepositoryException e) {
            throw new ProjectException("Failed to undelete project ''{0}''!", e, getName());
        }
    }

    public void unlock(WorkspaceUser user) throws ProjectException {
        if (!isLocked()) {
            throw new ProjectException("Cannot unlock non-locked project ''{0}''!", null, getName());
        }

        try {
            rulesProject.unlock(user);
        } catch (RRepositoryException e) {
            throw new ProjectException("Cannot unlock project ''{0}'': " + e.getMessage(), e, getName());
        }
    }

    @Override
    public void update(ProjectArtefact srcArtefact) throws ProjectException {
        Project srcProject = (Project) srcArtefact;
        super.update(srcArtefact);

        try {
            rulesProject.setDependencies(srcProject.getDependencies());
        } catch (RRepositoryException e) {
            throw new ProjectException("Cannot update dependencies!", e);
        }
    }
}
