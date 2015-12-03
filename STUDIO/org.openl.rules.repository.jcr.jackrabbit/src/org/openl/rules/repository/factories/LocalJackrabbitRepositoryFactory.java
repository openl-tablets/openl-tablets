package org.openl.rules.repository.factories;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeTypeManager;

import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.api.JackrabbitNodeTypeManager;
import org.apache.jackrabbit.core.TransientRepository;
import org.apache.jackrabbit.core.nodetype.NodeTypeManagerImpl;
import org.openl.config.ConfigPropertyString;
import org.openl.config.ConfigSet;
import org.openl.rules.common.impl.CommonVersionImpl;
import org.openl.rules.repository.RRepository;
import org.openl.rules.repository.RTransactionManager;
import org.openl.rules.repository.exceptions.RRepositoryException;
import org.openl.rules.repository.jcr.JcrNT;
import org.openl.rules.repository.jcr.JcrProductionRepository;
import org.openl.rules.repository.utils.UserUtil;
import org.openl.util.FileUtils;
import org.openl.util.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Local Jackrabbit Repository Factory. It handles own instance of Jackrabbit
 * repository.
 *
 * @author Aleh Bykhavets
 */
public class LocalJackrabbitRepositoryFactory extends AbstractJackrabbitRepositoryFactory {
    private final Logger log = LoggerFactory.getLogger(LocalJackrabbitRepositoryFactory.class);
    private static final String LOCK_FILE = ".lock";

    /**
     * Jackrabbit local repository
     */
    protected TransientRepository repository;
    protected File repHome;
    protected boolean convert = false;

    @Override
    protected void finalize() throws Throwable {
        try {
            release();
        } catch (RRepositoryException e) {
            try {
                log.error("finalize", e);
            } catch (Throwable ignored) {
            }
        } catch (Throwable ignored) {
        } finally {
            super.finalize();
        }
    }

    private static boolean isFileLocked(File file) {
        FileInputStream fileInputStream = null;
        try {
            fileInputStream = new FileInputStream(file);
            fileInputStream.read();
            return false;
        } catch (IOException e) {
            return true;
        } finally {
            IOUtils.closeQuietly(fileInputStream);
        }
    }

    protected static boolean isRepositoryLocked(File repositoryHome) {
        File lockFile = new File(repositoryHome, LOCK_FILE);
        return lockFile.exists() && isFileLocked(lockFile);
    }

    protected void createTransientRepo(File fullPath) throws RepositoryException {
        if (isRepositoryLocked(repHome)) {
            throw new RepositoryException("Repository is already locked.");
        }
        repository = new TransientRepository(fullPath, repHome);
    }

    // ------ private methods ------

    /**
     * Starts Jackrabbit repository. If there was no repository it will be
     * created automatically. (this is how Jacrabbit works)
     *
     * @throws RepositoryException if failed
     */
    private void init() throws RepositoryException {
        try {
            String jackrabbitConfig;
            if (StringUtils.isEmpty(login.getValue())) {
                jackrabbitConfig = "/jackrabbit-repository.xml";
            } else {
                jackrabbitConfig = "/secure-jackrabbit-repository.xml";
            }

            // obtain real path to repository configuration file
            InputStream input = this.getClass().getResourceAsStream(jackrabbitConfig);

            File tempRepositorySettings = File.createTempFile("jackrabbit-repository", ".xml");
            // It could be cleaned-up on exit
            tempRepositorySettings.deleteOnExit();

            OutputStream tempRepositorySettingsStream = new FileOutputStream(tempRepositorySettings);
            IOUtils.copyAndClose(input, tempRepositorySettingsStream);

            createTransientRepo(tempRepositorySettings);

            // Register shut down hook
            ShutDownHook shutDownHook = new ShutDownHook(this);
            Runtime.getRuntime().addShutdownHook(shutDownHook);
        } catch (IOException e) {
            throw new RepositoryException("Failed to init: " + e.getMessage(), e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initialize(ConfigSet confSet) throws RRepositoryException {
        super.initialize(confSet);

        repHome = new File(uri.getValue());

        try {
            init();
            setRepository(repository);
        } catch (RepositoryException e) {
            throw new RRepositoryException("Failed to initialize JCR: " + e.getMessage(), e);
        }
    }

    @Override
    protected void checkSchemaVersion(NodeTypeManager ntm) throws RepositoryException {
        String schemaVersion = getCurrentSchemaVersion(ntm);
        // compare expected and repository schema versions
        String expectedVersion = getExpectedSchemaVersion();
        if (!expectedVersion.equals(schemaVersion)) {
            // TODO Remove conversion sometimes
            if (ProductionRepositoryConvertor.from.compareTo(new CommonVersionImpl(schemaVersion)) == 0
                    && ProductionRepositoryConvertor.to.compareTo(new CommonVersionImpl(expectedVersion)) == 0) {
                convert = true;
                return;//success
            }
            throw new RepositoryException("Schema version is different. Has (" + schemaVersion + ") when ("
                    + expectedVersion + ") expected.");
        }
    }

    protected void convert() throws RRepositoryException {
        RRepository repositoryInstance = null;
        File tempRepoHome;
        try {
            repositoryInstance = super.getRepositoryInstance();
            tempRepoHome = FileUtils.createTempDirectory();
            //FIXME
            RepositoryConvertor repositoryConvertor = new RepositoryConvertor(confRulesProjectsLocation.getValue(),
                    confDeploymentProjectsLocation.getValue(), tempRepoHome);
            log.info("Converting repository. Please, be patient.");
            repositoryConvertor.convert(repositoryInstance);
        } catch (Exception e) {
            throw new RRepositoryException("Failed to convert repository.", e);
        } finally {
            if (repositoryInstance != null) {
                repositoryInstance.release();
            }
        }
        if (isProductionRepository()) {
            try {
                Session session = createSession();

                RTransactionManager transactionManager = getTrasactionManager(session);
                JcrProductionRepository productionRepository = new JcrProductionRepository(null, session,
                        transactionManager);
                ProductionRepositoryConvertor repositoryConvertor = new ProductionRepositoryConvertor(tempRepoHome);
                log.info("Converting production repository. Please, be patient.");
                repositoryConvertor.convert(productionRepository);
            } catch (Exception e) {
                throw new RRepositoryException("Failed to convert repository.", e);
            } finally {
                if (repositoryInstance != null) {
                    repositoryInstance.release();
                }
            }
        }
        try {
            FileUtils.delete(repHome);
            FileUtils.move(tempRepoHome, repHome);
        } catch (IOException e) {
            throw new RRepositoryException("Failed to convert repository.", e);
        }

    }

    private boolean isProductionRepository() {
        Session systemSession = null;
        try {
            systemSession = createSession();
            NodeTypeManager ntm = systemSession.getWorkspace().getNodeTypeManager();

            // Does JCR know anything about OpenL?
            return ntm.hasNodeType(JcrNT.NT_PROD_PROJECT);
        } catch (RepositoryException e) {
            return false;
        } finally {
            if (systemSession != null) {
                systemSession.logout();
            }
        }
    }

    @Override
    public RRepository createRepository() throws RRepositoryException {
        if (convert) {
            convert();
            convert = false;
        }

        return super.createRepository();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void initNodeTypes(NodeTypeManager ntm) throws RepositoryException {
        NodeTypeManagerImpl ntmi = (NodeTypeManagerImpl) ntm;

        try {
            InputStream is = null;
            try {
                is = this.getClass().getResourceAsStream(DEFAULT_NODETYPE_FILE);
                ntmi.registerNodeTypes(is, JackrabbitNodeTypeManager.TEXT_XML, true);
            } finally {
                if (is != null) {
                    is.close();
                }
            }
        } catch (IOException e) {
            throw new RepositoryException("Failed to init NodeTypes: " + e.getMessage(), e);
        }
    }

    public boolean configureJCRForOneUser(String login, String pass) {
        try {
            UserUtil userUtil = new UserUtil(repository);
            userUtil.createNewAdminUser(login, pass);
            userUtil.disableAnonymous();
            userUtil.changeAdminPass(pass);

            return true;
        } catch (Exception e) {
            return false;
        }
    }

}
