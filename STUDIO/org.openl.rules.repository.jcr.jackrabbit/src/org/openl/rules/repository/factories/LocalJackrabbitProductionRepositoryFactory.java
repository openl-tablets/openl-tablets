package org.openl.rules.repository.factories;

import java.io.File;
import java.io.IOException;

import javax.jcr.Session;

import org.openl.config.ConfigPropertyString;
import org.openl.rules.repository.RProductionRepository;
import org.openl.rules.repository.RTransactionManager;
import org.openl.rules.repository.exceptions.RRepositoryException;
import org.openl.rules.repository.jcr.JcrProductionRepository;
import org.openl.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LocalJackrabbitProductionRepositoryFactory extends LocalJackrabbitRepositoryFactory {

    private final Logger log = LoggerFactory.getLogger(LocalJackrabbitProductionRepositoryFactory.class);

    private final ConfigPropertyString confRepositoryName = new ConfigPropertyString("production-repository.name",
        "Local Jackrabbit");

    public LocalJackrabbitProductionRepositoryFactory() {
        setConfRepositoryName(confRepositoryName);
        setProductionRepositoryMode(true);
    }

    protected void convert() throws RRepositoryException {
        RProductionRepository repositoryInstance = null;
        File tempRepoHome;
        try {
            // FIXME: do not hardcode credential info
            Session session = createSession();
            RTransactionManager transactionManager = getTrasactionManager(session);
            repositoryInstance = new JcrProductionRepository(repositoryName, session, transactionManager);
            tempRepoHome = FileUtils.createTempDirectory();
            // FIXME
            ProductionRepositoryConvertor repositoryConvertor = new ProductionRepositoryConvertor(tempRepoHome);
            log.info("Converting production repository. Please, be patient.");
            repositoryConvertor.convert(repositoryInstance);
        } catch (Exception e) {
            throw new RRepositoryException("Failed to convert repository.", e);
        } finally {
            if (repositoryInstance != null) {
                repositoryInstance.release();
            }
        }

        try {
            FileUtils.delete(repHome);
            FileUtils.move(tempRepoHome, repHome);
        } catch (IOException e) {
            throw new RRepositoryException("Failed to convert repository.", e);
        }
    }
}
