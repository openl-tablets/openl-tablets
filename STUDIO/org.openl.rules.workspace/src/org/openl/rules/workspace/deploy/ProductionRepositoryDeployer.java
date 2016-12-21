package org.openl.rules.workspace.deploy;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Map;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.openl.config.ConfigurationManagerFactory;
import org.openl.rules.repository.RepositoryFactoryInstatiator;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.workspace.WorkspaceUser;
import org.openl.rules.workspace.WorkspaceUserImpl;
import org.openl.util.FileUtils;
import org.openl.util.IOUtils;
import org.openl.util.ZipUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;

/**
 * This class allows to deploy a zip-based project to a production repository.
 * By default configuration of destination repository is get from
 * "deployer.properties" file.
 *
 * @author Yury Molchan
 */
public class ProductionRepositoryDeployer {
    // Some user name for JCR
    private static WorkspaceUser user = new WorkspaceUserImpl("OpenL_Deployer");
    private final Logger log = LoggerFactory.getLogger(ProductionRepositoryDeployer.class);

    /**
     * Deploys a new project to the production repository. If the project exists
     * then it will be skipped to deploy.
     *
     * @param zipFile the project to deploy
     * @param config the configuration file name
     * @throws Exception
     */
    public void deploy(File zipFile, String config) throws Exception {
        if (config == null || config.isEmpty()) {
            config = "deployer.properties";
        }
        ConfigurationManagerFactory configManagerFactory = new ConfigurationManagerFactory(true, null, "");
        ;
        Map<String, Object> properties = configManagerFactory.getConfigurationManager(config).getProperties();
        deployInternal(zipFile, properties, true);
    }

    /**
     * Deploys a new or redeploys an existing project to the production
     * repository.
     *
     * @param zipFile the project to deploy
     * @param config the configuration file name
     * @throws Exception
     */
    public void redeploy(File zipFile, String config) throws Exception {
        if (config == null || config.isEmpty()) {
            config = "deployer.properties";
        }
        ConfigurationManagerFactory configManagerFactory = new ConfigurationManagerFactory(true, null, "");
        ;
        Map<String, Object> properties = configManagerFactory.getConfigurationManager(config).getProperties();
        deployInternal(zipFile, properties, false);
    }

    public void deployInternal(File zipFile, Map<String, Object> properties, boolean skipExist) throws Exception {

        // Temp folders
        File zipFolder = FileUtils.createTempDirectory();
        Repository deployRepo = null;
        try {
            // Initialize repo
            deployRepo = RepositoryFactoryInstatiator.newFactory(properties, false);
            ;

            // Wait 15 seconds for initializing networking in JGroups.
            Object initializeTimeout = properties.get("timeout.networking.initialize");
            Thread.sleep(initializeTimeout == null ? 15000 : Integer.parseInt(initializeTimeout.toString()));

            String name = FileUtils.getBaseName(zipFile.getName());

            // Unpack jar to a file system
            ZipUtils.extractAll(zipFile, zipFolder);

            // Renamed a project according to rules.xml
            File rules = new File(zipFolder, "rules.xml");
            if (rules.exists()) {
                String rulesName = getProjectName(rules);
                if (rulesName != null && !rulesName.isEmpty()) {
                    name = rulesName;
                }
            }

            int version = DeployUtils.getNextDeploymentVersion(deployRepo, name);
            if (version > 1 && skipExist) {
                log.info("Project [{}] exists. It has been skipped to deploy.", name);
                return;
            }

            // Do deploy
            String target = new StringBuilder(DeployUtils.DEPLOY_PATH).append(name)
                .append('#')
                .append(version)
                .append('/')
                .append(name)
                .toString();
            FileData dest = new FileData();
            dest.setName(target);
            dest.setAuthor("OpenL_Deployer");
            deployRepo.save(dest, new FileInputStream(zipFile));

            // Wait 10 seconds for finalizing networking in JGroups.
            // + 30 seconds for Infinispan.
            // This time should exceed Infinispan timeouts.
            Object finalizeTimeout = properties.get("timeout.networking.finalize");
            Thread.sleep(finalizeTimeout == null ? 40000 : Integer.parseInt(finalizeTimeout.toString()));
        } finally {
            /* Clean up */
            FileUtils.deleteQuietly(zipFolder);
            // Close repo
            if (deployRepo != null) {
                if (deployRepo instanceof Closeable) {
                    // Close repo connection after validation
                    IOUtils.closeQuietly((Closeable) deployRepo);
                }
            }
        }
    }

    private String getProjectName(File file) {
        try {
            InputSource inputSource = new InputSource(new FileInputStream(file));
            XPathFactory factory = XPathFactory.newInstance();
            XPath xPath = factory.newXPath();
            XPathExpression xPathExpression = xPath.compile("/project/name");
            return xPathExpression.evaluate(inputSource);
        } catch (FileNotFoundException e) {
            return null;
        } catch (XPathExpressionException e) {
            return null;
        }
    }
}
