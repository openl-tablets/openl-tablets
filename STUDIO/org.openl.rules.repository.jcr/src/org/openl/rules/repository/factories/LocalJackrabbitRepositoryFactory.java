package org.openl.rules.repository.factories;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeTypeManager;

import org.apache.jackrabbit.core.TransientRepository;
import org.apache.jackrabbit.core.nodetype.NodeTypeManagerImpl;
import org.openl.rules.repository.SmartProps;
import org.openl.rules.repository.exceptions.RRepositoryException;

/**
 * Local Jackrabbit Repository Factory.
 * It handles own instance of Jackrabbit repository.
 * 
 * @author Aleh Bykhavets
 *
 */
public class LocalJackrabbitRepositoryFactory extends AbstractJcrRepositoryFactory {
    public static final String PROP_REPOSITORY_HOME = "JCR.local.home";
    public static final String DEFAULT_REPOSITORY_HOME = "../local-repository";
    public static final String PROP_REPOSITORY_NAME = "JCR.name";
    public static final String DEFAULT_REPOSITORY_NAME = "Jackrabbit Local";

    /** Jackrabbit local repository */
    private TransientRepository repository;
    private String repHome;

    /** {@inheritDoc} */
    public void initialize(SmartProps props) throws RRepositoryException {
        super.initialize(props);

        String repName = props.getStr(PROP_REPOSITORY_NAME, DEFAULT_REPOSITORY_NAME);
        repHome = props.getStr(PROP_REPOSITORY_HOME, DEFAULT_REPOSITORY_HOME);

        try {
            init();
            setRepository(repository, repName);
        } catch (RepositoryException e) {
            throw new RRepositoryException("Failed to initialize", e);
        }
    }

    /**
     * Shutdown local JCR Repository.
     * Release all allocated resources.
     * <p>
     * Note: There is no 100% that {@link #finalize()} will be invoked by JVM
     */
    @Override
    protected void finalize() throws Throwable {
        // TODO: close open sessions
        repository.shutdown();
        super.finalize();
    }

    // ------ private methods ------

    /**
     * Starts Jackrabbit repository.
     * If there was no repository it will be created automatically.
     * (this is how Jacrabbit works)
     *
     * @throws RepositoryException if failed
     */
    private void init() throws RepositoryException {
        try {
            String repConf = "/jackrabbit-repository.xml";
            
            // obtain real path to repository configuration file
            URL url = this.getClass().getResource(repConf);
            String fullPath = url.getFile();

            repository = new TransientRepository(fullPath, repHome);
        } catch (IOException e) {
            // TODO: log
            throw new RepositoryException("Failed to init: " + e.getMessage(), e);
        }
    }

    /** {@inheritDoc} */
    protected void initNodeTypes(NodeTypeManager ntm) throws RepositoryException {
        NodeTypeManagerImpl ntmi = (NodeTypeManagerImpl) ntm;

        try {
            InputStream is = null;
            try {
                is = this.getClass().getResourceAsStream("/org/openl/rules/repository/openl_nodetypes.xml");
                ntmi.registerNodeTypes(is, NodeTypeManagerImpl.TEXT_XML, true);
            } finally {
                if (is != null) {
                    is.close();
                }
            }
        } catch (IOException e) {
            // TODO: add 2 log
            throw new RepositoryException("Failed to init NodeTypes: " + e.getMessage(), e);
        }
    }
}
