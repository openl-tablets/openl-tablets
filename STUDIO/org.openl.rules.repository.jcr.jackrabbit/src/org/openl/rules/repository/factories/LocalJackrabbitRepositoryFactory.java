package org.openl.rules.repository.factories;

import static org.apache.commons.io.FileUtils.getTempDirectoryPath;

import org.apache.commons.io.FileUtils;
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
import org.openl.util.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeTypeManager;
import java.io.*;
import java.net.URL;

/**
 * Local Jackrabbit Repository Factory. It handles own instance of Jackrabbit
 * repository.
 *
 * @author Aleh Bykhavets
 */
public class LocalJackrabbitRepositoryFactory extends AbstractJackrabbitRepositoryFactory {
    private final Logger log = LoggerFactory.getLogger(LocalJackrabbitRepositoryFactory.class);
    private static final String LOCK_FILE = ".lock";

    private ConfigPropertyString confRepositoryHome = new ConfigPropertyString(
            "repository.local.home", "../local-repository");
    private ConfigPropertyString confRepositoryName = new ConfigPropertyString(
            "repository.name", "Local Jackrabbit");

    /**
     * Jackrabbit local repository
     */
    protected TransientRepository repository;
    protected String repHome;
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

    protected static boolean isRepositoryLocked(String repositoryHome) {
        File lockFile = new File(repositoryHome, LOCK_FILE);
        return lockFile.exists() && isFileLocked(lockFile);
    }

    protected void createTransientRepo(String fullPath) throws RepositoryException {
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
            String repConf = this.getRepoConfigFile().getValue();//"/jackrabbit-repository.xml";

            // obtain real path to repository configuration file
            URL url = this.getClass().getResource(repConf);

            File tempRepositorySettings = File.createTempFile("jackrabbit-repository", ".xml");
            // It could be cleaned-up on exit
            tempRepositorySettings.deleteOnExit();

            String fullPath = tempRepositorySettings.getCanonicalPath();

            OutputStream tempRepositorySettingsStream = new FileOutputStream(tempRepositorySettings);
            IOUtils.copyAndClose(url.openStream(), tempRepositorySettingsStream);

            createTransientRepo(fullPath);

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

        confSet.updateProperty(confRepositoryHome);
        confSet.updateProperty(confRepositoryName);

        repHome = confRepositoryHome.getValue();

        // resolve "." and "..", if any
        try {
            File f = new File(repHome);
            repHome = f.getCanonicalPath();
        } catch (IOException e) {
            log.error("Failed to get canonical path for repository home ({})", repHome, e);
        }

        try {
            init();
            setRepository(repository, confRepositoryName.getValue());
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
        String tempRepoHome = getTempDirectoryPath() + "/.openl/repo/";
        try {
            repositoryInstance = super.getRepositoryInstance();
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
                JcrProductionRepository productionRepository = new JcrProductionRepository(repositoryName, session,
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

    public ConfigPropertyString getConfRepositoryHome() {
        return confRepositoryHome;
    }

    public void setConfRepositoryHome(ConfigPropertyString confRepositoryHome) {
        this.confRepositoryHome = confRepositoryHome;
    }

    public ConfigPropertyString getConfRepositoryName() {
        return confRepositoryName;
    }

    public void setConfRepositoryName(ConfigPropertyString confRepositoryName) {
        this.confRepositoryName = confRepositoryName;
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
