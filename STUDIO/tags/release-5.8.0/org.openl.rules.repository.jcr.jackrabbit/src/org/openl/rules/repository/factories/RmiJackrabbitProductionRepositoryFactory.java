package org.openl.rules.repository.factories;

import org.openl.config.ConfigPropertyString;
import org.openl.rules.repository.RProductionRepository;
import org.openl.rules.repository.RTransactionManager;
import org.openl.rules.repository.jcr.JcrProductionRepository;
import org.openl.rules.repository.exceptions.RRepositoryException;

import javax.jcr.Session;
import javax.jcr.RepositoryException;

public class RmiJackrabbitProductionRepositoryFactory extends RmiJackrabbitRepositoryFactory {

    private ConfigPropertyString confRmiUrl = new ConfigPropertyString(
            "production-repository.remote.rmi.url", "//localhost:1099/jackrabbit.repository");

    public RmiJackrabbitProductionRepositoryFactory() {
        setConfRmiUrl(confRmiUrl);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RProductionRepository getRepositoryInstance() throws RRepositoryException {
        try {
            // FIXME: do not hardcode credential info
            Session session = createSession("user", "pass");
            RTransactionManager transactionManager = getTrasactionManager(session);
            return new JcrProductionRepository(repositoryName, session, transactionManager);
        } catch (RepositoryException e) {
            throw new RRepositoryException("Failed to get Repository Instance", e);
        }
    }
}
