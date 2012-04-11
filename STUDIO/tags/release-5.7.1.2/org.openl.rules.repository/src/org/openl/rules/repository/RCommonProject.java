package org.openl.rules.repository;

import org.openl.rules.repository.exceptions.RRepositoryException;

public interface RCommonProject {
    /**
     * Commits changes in background versioned storage.
     *
     * @throws RRepositoryException if failed
     */
    public void commit(CommonUser user) throws RRepositoryException;

    /**
     * Marks the project for deletion. Project is too important to be deleted so
     * easily.
     *
     * @throws RDeleteException if failed
     */
    public void delete(CommonUser user) throws RRepositoryException;

    /**
     * Erases the project from the repository completely. Before erasing the
     * project must be marked for deletion. I.e. {@link #delete()} should be
     * invoked. Otherwise this method will throw exception.
     *
     * @throws RDeleteException if failed
     */
    public void erase(CommonUser user) throws RRepositoryException;

    public RLock getLock() throws RRepositoryException;

    /**
     * Gets name of the project.
     *
     * @return project's name
     */
    public String getName();

    public boolean isLocked() throws RRepositoryException;

    /**
     * Returns whether the project is marked for deletion. If a project is
     * marked for deletion, it should not be used.
     *
     * @return <code>true</code> if project is marked for deletion;
     *         <code>false</code> otherwise
     */
    public boolean isMarked4Deletion() throws RRepositoryException;

    public void lock(CommonUser user) throws RRepositoryException;

    public void riseVersion(int major, int minor) throws RRepositoryException;

    /**
     * Unmarks the project from deletion.
     *
     * @throws RModifyException if failed
     */
    public void undelete(CommonUser user) throws RRepositoryException;

    public void unlock(CommonUser user) throws RRepositoryException;
}
