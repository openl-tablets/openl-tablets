package org.openl.rules.repository.factories;

import org.openl.rules.repository.RProductionRepository;
import org.openl.rules.repository.jcr.JcrProductionRepository;
import org.openl.rules.repository.exceptions.RRepositoryException;

import javax.jcr.Session;
import javax.jcr.RepositoryException;

public class RmiJackrabbitProductionRepositoryFactory extends RmiJackrabbitRepositoryFactory {
    /**
     * {@inheritDoc}
     */
    @Override
    public RProductionRepository getRepositoryInstance() throws RRepositoryException {
        try {
            // FIXME: do not hardcode credential info
            Session session = createSession("user", "pass");

            return new JcrProductionRepository(repositoryName, session);
        } catch (RepositoryException e) {
            throw new RRepositoryException("Failed to get Repository Instance", e);
        }
    }
}
