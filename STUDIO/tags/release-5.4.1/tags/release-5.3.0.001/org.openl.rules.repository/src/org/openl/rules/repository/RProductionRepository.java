package org.openl.rules.repository;

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

    void addListener(RDeploymentListener listener) throws RRepositoryException;

    /**
     * Creates new deployment in repository.
     *
     * @param name the name of the new deployment
     * @return newly created deployment
     * @throws RRepositoryException if the deployment with given name already
     *             exists or on an error working with the repository
     */
    RProductionDeployment createDeployment(String name) throws RRepositoryException;

    Collection<REntity> findNodes(SearchParams params) throws RRepositoryException;

    /**
     * Gets deployment by name.
     *
     * @param name the name of the deployment
     * @return the deployment with given name
     * @throws RRepositoryException on error working with repository
     */
    RProductionDeployment getDeployment(String name) throws RRepositoryException;

    /**
     * Returns names for all deployments in the repository.
     *
     * @return collection of all deployment names.
     * @throws RRepositoryException on error working with repository
     */
    Collection<String> getDeploymentNames() throws RRepositoryException;

    /**
     * Checks if a deployment with given name exists in the repository.
     *
     * @param name deployment name
     * @return if the deployemnt with name <code>name</code> exists
     * @throws RRepositoryException on error working with the repository
     */
    boolean hasDeployment(String name) throws RRepositoryException;

    boolean removeListener(RDeploymentListener listener) throws RRepositoryException;
}
