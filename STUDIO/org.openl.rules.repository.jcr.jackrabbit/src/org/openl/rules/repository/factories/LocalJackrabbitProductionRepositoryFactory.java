package org.openl.rules.repository.factories;

import static org.apache.commons.io.FileUtils.getTempDirectoryPath;

import java.io.File;
import java.io.IOException;

import javax.jcr.Session;

import org.apache.commons.io.FileUtils;
import org.openl.config.ConfigPropertyString;
import org.openl.rules.repository.RProductionRepository;
import org.openl.rules.repository.RTransactionManager;
import org.openl.rules.repository.exceptions.RRepositoryException;
import org.openl.rules.repository.jcr.JcrProductionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LocalJackrabbitProductionRepositoryFactory extends LocalJackrabbitRepositoryFactory {

    private final Logger log = LoggerFactory.getLogger(LocalJackrabbitProductionRepositoryFactory.class);

    private final ConfigPropertyString confRepositoryHome = new ConfigPropertyString("production-repository.local.home",
        "../local-repository");
    private final ConfigPropertyString confRepositoryName = new ConfigPropertyString("production-repository.name",
        "Local Jackrabbit");
    private final ConfigPropertyString login = new ConfigPropertyString("production-repository.login", "user");
    private final ConfigPropertyString password = new ConfigPropertyString("production-repository.password", "pass");
    private final ConfigPropertyString repoConfigFile = new ConfigPropertyString("production-repository.config",
        "/jackrabbit-repository.xml");

    public LocalJackrabbitProductionRepositoryFactory() {
        setUri(confRepositoryHome);
        setConfRepositoryName(confRepositoryName);
        setLogin(login);
        setPassword(password);
        setRepoConfigFile(repoConfigFile);
        setProductionRepositoryMode(true);
    }

    protected void convert() throws RRepositoryException {
        RProductionRepository repositoryInstance = null;
        String tempRepoHome = getTempDirectoryPath() + "/.openl/prod_repo/";
        try {
            // FIXME: do not hardcode credential info
            Session session = createSession();
            RTransactionManager transactionManager = getTrasactionManager(session);
            repositoryInstance = new JcrProductionRepository(repositoryName, session, transactionManager);
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

        File repoHome = new File(repHome);
        File tmpRepoHome = new File(tempRepoHome);
        try {
            FileUtils.deleteDirectory(repoHome);
            FileUtils.copyDirectory(tmpRepoHome, repoHome);
        } catch (IOException e) {
            throw new RRepositoryException("Failed to convert repository.", e);
        } finally {
            FileUtils.deleteQuietly(tmpRepoHome);
        }
    }
}
