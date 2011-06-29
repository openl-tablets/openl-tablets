package org.openl.rules.repository.factories;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.openl.config.ConfigPropertyString;
import org.openl.rules.repository.RProductionRepository;
import org.openl.rules.repository.RTransactionManager;
import org.openl.rules.repository.exceptions.RRepositoryException;
import org.openl.rules.repository.jcr.JcrProductionRepository;

/**
 * @author PUdalau
 */
public class WebDavJackrabbitProductionRepositoryFactory extends WebDavJacrabbitRepositoryFactory {

    private ConfigPropertyString confWebdavUrl = new ConfigPropertyString(
            "production-repository.remote.webdav.url", "http://localhost:8080/jcr/server/");

    public WebDavJackrabbitProductionRepositoryFactory() {
        setConfWebdavUrl(confWebdavUrl);
    }

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
