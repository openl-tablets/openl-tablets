package org.openl.rules.repository;

import org.openl.config.ConfigSet;
import org.openl.rules.repository.exceptions.RRepositoryException;

/**
 * Interface for concrete repository factories.
 *
 * @author Aleh Bykhavets
 *
 */
public interface RRepositoryFactory {

    /**
     * Gets new instance of JCR Repository.
     *
     * @return new instance of JCR Repository
     * @throws RRepositoryException if failed
     */
    RRepository getRepositoryInstance() throws RRepositoryException;

    /**
     * Initialize factory.
     *
     * @param confSet configuration data
     * @param designMode
     * @throws RRepositoryException if failed
     */
    void initialize(ConfigSet confSet, boolean designMode) throws RRepositoryException;

    void release() throws RRepositoryException;

}
