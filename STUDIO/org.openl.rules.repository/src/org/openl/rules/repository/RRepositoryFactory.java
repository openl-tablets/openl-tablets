package org.openl.rules.repository;

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
    public RRepository getRepositoryInstance() throws RRepositoryException;

    /**
     * Initialize factory.
     *
     * @param props properties
     * @throws RRepositoryException if failed
     */
    public void initialize(SmartProps props) throws RRepositoryException;

    void release() throws RRepositoryException;
}
