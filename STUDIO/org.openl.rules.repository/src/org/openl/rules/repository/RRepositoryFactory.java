package org.openl.rules.repository;

import org.openl.rules.repository.api.Repository;
import org.openl.rules.repository.exceptions.RRepositoryException;

/**
 * Interface for concrete repository factories.
 *
 * @author Aleh Bykhavets
 *
 */
public interface RRepositoryFactory extends Repository {

    /**
     * Initialize factory.
     */
    void initialize() throws RRepositoryException;
}
