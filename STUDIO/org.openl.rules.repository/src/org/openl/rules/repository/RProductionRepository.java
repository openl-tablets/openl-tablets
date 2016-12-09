package org.openl.rules.repository;

import org.openl.rules.repository.api.ArtefactAPI;
import org.openl.rules.repository.exceptions.RRepositoryException;

import java.util.Collection;
import java.util.Date;

/**
 * OpenL projects production repositury, that is the place where OpenL projects
 * are deployed to.
 */
public interface RProductionRepository extends RRepository {
    interface SearchParams {
        String getLineOfBusiness();

        Date getLowerEffectiveDate();

        Date getLowerExpirationDate();

        Date getUpperEffectiveDate();

        Date getUpperExpirationDate();
    }

    void addListener(RDeploymentListener listener);

    Collection<ArtefactAPI> findNodes(SearchParams params) throws RRepositoryException;

    boolean removeListener(RDeploymentListener listener);

    /**
     * Notify production repository about changes.
     * 
     * @throws RRepositoryException
     */
    void notifyChanges() throws RRepositoryException;
}
