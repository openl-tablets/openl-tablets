package org.openl.rules.dtr;

import org.openl.rules.commons.projects.Project;
import org.openl.rules.commons.projects.ProjectException;
import org.openl.rules.WorkspaceUser;

public interface RepositoryProject extends Project, RepositoryProjectFolder {
    void lock(WorkspaceUser user) throws ProjectException;
    void unlock(WorkspaceUser user) throws ProjectException;
    void delete() throws ProjectException;
    void undelete() throws ProjectException;
    void erase() throws ProjectException;

    boolean isMarkedForDeletion();

    boolean isLocked();
    LockInfo getlLockInfo();
}
