package org.openl.rules.repository;

import org.openl.rules.repository.exceptions.RRepositoryException;

/**
 * OpenL projects production repositury, that is the place where OpenL projects
 * are deployed to.
 */
public interface RProductionRepository extends RRepository {
    void addListener(RDeploymentListener listener);

    boolean removeListener(RDeploymentListener listener);

    /**
     * Notify production repository about changes.
     * 
     * @throws RRepositoryException
     */
    void notifyChanges() throws RRepositoryException;
}
