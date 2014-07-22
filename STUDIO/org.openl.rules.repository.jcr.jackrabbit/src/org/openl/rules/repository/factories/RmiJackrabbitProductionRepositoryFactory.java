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
    private final ConfigPropertyString login = new ConfigPropertyString(
            "production-repository.login", "user");
    private final ConfigPropertyString password = new ConfigPropertyString(
            "production-repository.password", "pass");
    private final ConfigPropertyString repoConfigFile = new ConfigPropertyString(
            "production-repository.config", "/jackrabbit-repository.xml");

    public RmiJackrabbitProductionRepositoryFactory() {
        setConfRmiUrl(confRmiUrl);
        setLogin(login);
        setPassword(password);
        setRepoConfigFile(repoConfigFile);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RProductionRepository createRepository() throws RRepositoryException {
        try {
            // FIXME: do not hardcode credential info
            Session session = createSession();
            RTransactionManager transactionManager = getTrasactionManager(session);
            return new JcrProductionRepository(repositoryName, session, transactionManager);
        } catch (RepositoryException e) {
            throw new RRepositoryException("Failed to get Repository Instance", e);
        }
    }
}
