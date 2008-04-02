package org.openl.rules.repository.factories;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeTypeManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.core.TransientRepository;
import org.apache.jackrabbit.core.nodetype.NodeTypeManagerImpl;
import org.openl.config.ConfigPropertyString;
import org.openl.config.ConfigSet;
import org.openl.rules.repository.exceptions.RRepositoryException;
import org.springframework.util.FileCopyUtils;

/**
 * Local Jackrabbit Repository Factory. It handles own instance of Jackrabbit
 * repository.
 * 
 * @author Aleh Bykhavets
 * 
 */
public class LocalJackrabbitRepositoryFactory extends AbstractJcrRepositoryFactory {
    private static Log log = LogFactory.getLog(LocalJackrabbitRepositoryFactory.class);

    public static final String DEFAULT_NODETYPE_FILE = "/org/openl/rules/repository/openl_nodetypes.xml";

    private final ConfigPropertyString confRepositoryHome = new ConfigPropertyString("repository.jackrabbit.local.home",
            "../local-repository");
    private final ConfigPropertyString confNodeTypeFile = new ConfigPropertyString("repository.jcr.nodetypes",
            DEFAULT_NODETYPE_FILE);
    private final ConfigPropertyString confRepositoryName = new ConfigPropertyString("repository.name", "Local Jackrabbit");

    /** Jackrabbit local repository */
    protected TransientRepository repository;
    private String repHome;
    private String nodeTypeFile;

    /** {@inheritDoc} */
    public void initialize(ConfigSet confSet) throws RRepositoryException {
        super.initialize(confSet);

        confSet.updateProperty(confRepositoryHome);
        confSet.updateProperty(confNodeTypeFile);
        confSet.updateProperty(confRepositoryName);

        repHome = confRepositoryHome.getValue();
        nodeTypeFile = confNodeTypeFile.getValue();

        // resolve "." and "..", if any
        try {
            File f = new File(repHome);
            repHome = f.getCanonicalPath();
        } catch (IOException e) {
            log.error("Failed to get canonical path for repository home (" + repHome + ")", e);
        }

        try {
            init();
            setRepository(repository, confRepositoryName.getValue());
        } catch (RepositoryException e) {
            throw new RRepositoryException("Failed to initialize JCR: " + e.getMessage(), e);
        }
    }

    // ------ private methods ------

    /**
     * Starts Jackrabbit repository. If there was no repository it will be
     * created automatically. (this is how Jacrabbit works)
     * 
     * @throws RepositoryException
     *                 if failed
     */
    private void init() throws RepositoryException {
        try {
            String repConf = "/jackrabbit-repository.xml";

            // obtain real path to repository configuration file
            URL url = this.getClass().getResource(repConf);

            File tempRepositorySettings = File.createTempFile("jackrabbit-repository", ".xml");
            // It could be cleaned-up on exit
            tempRepositorySettings.deleteOnExit();

            String fullPath = tempRepositorySettings.getCanonicalPath();

            OutputStream tempRepositorySettingsStream = new FileOutputStream(tempRepositorySettings);
            FileCopyUtils.copy(url.openStream(), tempRepositorySettingsStream);
            tempRepositorySettingsStream.close();

            repository = new TransientRepository(fullPath, repHome);

            Runtime.getRuntime().addShutdownHook(new Thread() {
                public void run() {
                    // shutdown gracefully
                    if (repository != null)
                        repository.shutdown();
                }
            });
        } catch (IOException e) {
            throw new RepositoryException("Failed to init: " + e.getMessage(), e);
        }
    }

    /** {@inheritDoc} */
    protected void initNodeTypes(NodeTypeManager ntm) throws RepositoryException {
        NodeTypeManagerImpl ntmi = (NodeTypeManagerImpl) ntm;

        try {
            InputStream is = null;
            try {
                is = this.getClass().getResourceAsStream(nodeTypeFile);
                ntmi.registerNodeTypes(is, NodeTypeManagerImpl.TEXT_XML, true);
            } finally {
                if (is != null) {
                    is.close();
                }
            }
        } catch (IOException e) {
            throw new RepositoryException("Failed to init NodeTypes: " + e.getMessage(), e);
        }
    }
}
