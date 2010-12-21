package org.openl.rules.repository.factories;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeTypeManager;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.api.JackrabbitNodeTypeManager;
import org.apache.jackrabbit.core.TransientRepository;
import org.apache.jackrabbit.core.nodetype.NodeTypeManagerImpl;
import org.openl.config.ConfigPropertyString;
import org.openl.config.ConfigSet;
import org.openl.rules.common.impl.CommonVersionImpl;
import org.openl.rules.repository.RRepository;
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

    private final ConfigPropertyString confRepositoryHome = new ConfigPropertyString(
            "repository.jackrabbit.local.home", "../local-repository");
    private final ConfigPropertyString confNodeTypeFile = new ConfigPropertyString("repository.jcr.nodetypes",
            DEFAULT_NODETYPE_FILE);
    private final ConfigPropertyString confRepositoryName = new ConfigPropertyString("repository.name",
            "Local Jackrabbit");

    /** Jackrabbit local repository */
    private TransientRepository repository;
    protected String repHome;
    private String nodeTypeFile;
    private ShutDownHook shutDownHook;
    protected boolean convert = false;

    @Override
    protected void finalize() {
        try {
            release();
        } catch (RRepositoryException e) {
            log.error("finalize", e);
        }

        if (shutDownHook != null) {
            Runtime.getRuntime().removeShutdownHook(shutDownHook);
        }
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

            // Register shut down hook
            ShutDownHook shutDownHook = new ShutDownHook(this);
            Runtime.getRuntime().addShutdownHook(shutDownHook);
        } catch (IOException e) {
            throw new RepositoryException("Failed to init: " + e.getMessage(), e);
        }
    }

    /** {@inheritDoc} */
    @Override
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
    
    protected void convert() throws RRepositoryException{
        RRepository repositoryInstance = null;
        String tempRepoHome = "/temp/repo/";
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
        return;
        
    }
    
    @Override
    public RRepository getRepositoryInstance() throws RRepositoryException {
        if(convert){
            convert();
            convert = false;
        }
        // TODO Auto-generated method stub
        return super.getRepositoryInstance();
    }
    
    /** {@inheritDoc} */
    @Override
    protected void initNodeTypes(NodeTypeManager ntm) throws RepositoryException {
        NodeTypeManagerImpl ntmi = (NodeTypeManagerImpl) ntm;

        try {
            InputStream is = null;
            try {
                is = this.getClass().getResourceAsStream(nodeTypeFile);
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

    @Override
    public void release() throws RRepositoryException {
        if (repository != null) {
            repository.shutdown();
            repository = null;
        }
    }
}
