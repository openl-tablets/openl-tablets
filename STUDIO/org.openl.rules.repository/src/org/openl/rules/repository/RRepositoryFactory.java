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
    RRepository getRepositoryInstance() throws RRepositoryException;

    /**
     * Initialize factory.
     *
     * @param uri
     * @param login
     * @param password @throws RRepositoryException if failed
     * @param designMode
     * */
    void initialize(String uri, String login, String password, boolean designMode) throws RRepositoryException;

    void release() throws RRepositoryException;

}
