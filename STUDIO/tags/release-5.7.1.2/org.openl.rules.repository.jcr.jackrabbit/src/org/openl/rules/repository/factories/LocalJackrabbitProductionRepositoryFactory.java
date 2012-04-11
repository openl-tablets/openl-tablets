package org.openl.rules.repository.factories;

import org.openl.rules.repository.RProductionRepository;
import org.openl.rules.repository.exceptions.RRepositoryException;
import org.openl.rules.repository.jcr.JcrProductionRepository;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

public class LocalJackrabbitProductionRepositoryFactory extends LocalJackrabbitRepositoryFactory {
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
