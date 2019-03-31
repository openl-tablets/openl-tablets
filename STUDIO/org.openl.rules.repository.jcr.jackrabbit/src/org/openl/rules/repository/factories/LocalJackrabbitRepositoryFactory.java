package org.openl.rules.repository.factories;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeTypeManager;

import org.apache.jackrabbit.api.JackrabbitNodeTypeManager;
import org.apache.jackrabbit.core.TransientRepository;
import org.apache.jackrabbit.core.nodetype.NodeTypeManagerImpl;
import org.openl.rules.repository.exceptions.RRepositoryException;
import org.openl.rules.repository.utils.UserUtil;
import org.openl.util.IOUtils;
import org.openl.util.StringUtils;

/**
 * Local Jackrabbit Repository Factory. It handles own instance of Jackrabbit repository.
 *
 * @author Aleh Bykhavets
 */
public class LocalJackrabbitRepositoryFactory extends AbstractJcrRepositoryFactory {
    private static final String LOCK_FILE = ".lock";

    /**
     * Jackrabbit local repository
     */
    protected TransientRepository repository;
    protected File repHome;

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
     * Starts Jackrabbit repository. If there was no repository it will be created automatically. (this is how Jacrabbit
     * works)
     *
     * @throws RepositoryException if failed
     */
    private void init() throws RepositoryException {
        try {
            String jackrabbitConfig;
            if (StringUtils.isEmpty(login)) {
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
        } catch (IOException e) {
            throw new RepositoryException("Failed to init: " + e.getMessage(), e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initialize() throws RRepositoryException {
        repHome = new File(this.uri);

        try {
            init();
            setRepository(repository);
        } catch (RepositoryException e) {
            throw new RRepositoryException("Failed to initialize JCR: " + e.getMessage(), e);
        }
        super.initialize();
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
