package org.openl.rules.workspace.deploy;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Map;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.openl.config.ConfigurationManagerFactory;
import org.openl.rules.common.impl.ArtefactPathImpl;
import org.openl.rules.project.abstraction.ADeploymentProject;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.repository.ProductionRepositoryFactoryProxy;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.repository.file.FileRepository;
import org.openl.rules.workspace.WorkspaceUser;
import org.openl.rules.workspace.WorkspaceUserImpl;
import org.openl.rules.workspace.deploy.impl.jcr.JcrProductionDeployer;
import org.openl.rules.workspace.lw.impl.LocalWorkspaceImpl;
import org.openl.util.FileUtils;
import org.openl.util.ZipUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;

/**
 * This class allows to deploy a zip-based project to a production repository.
 * By default configuration of destination repository is get from "deployer.properties" file.
 *
 * @author Yury Molchan
 */
public class ProductionRepositoryDeployer {
    // Some user name for JCR
    private static WorkspaceUser user = new WorkspaceUserImpl("OpenL_Deployer");
    private final Logger log = LoggerFactory.getLogger(ProductionRepositoryDeployer.class);

    /**
     * Deploys a new project to the production repository. If the project exists then it will be skipped to deploy.
     *
     * @param zipFile the project to deploy
     * @param config  the configuration file name
     * @throws Exception
     */
    public void deploy(File zipFile, String config) throws Exception {
        deployInternal(zipFile, config, true);
    }

    /**
     * Deploys a new or redeploys an existing project to the production repository.
     *
     * @param zipFile the project to deploy
     * @param config  the configuration file name
     * @throws Exception
     */
    public void redeploy(File zipFile, String config) throws Exception {
        deployInternal(zipFile, config, false);
    }

    private void deployInternal(File zipFile, String config, boolean skipExist) throws Exception {
        if (config == null || config.isEmpty()) {
            config = "deployer.properties";
        }

        // Initialize repo
        ProductionRepositoryFactoryProxy repositoryFactoryProxy = new ProductionRepositoryFactoryProxy();
        JcrProductionDeployer deployer = new JcrProductionDeployer(repositoryFactoryProxy, config);

        // Temp folders
        File tempDirectory = FileUtils.createTempDirectory();

        String name = FileUtils.getBaseName(zipFile.getName());

        File workspaceLocation = new File(tempDirectory, "workspace");
        workspaceLocation.mkdirs();
        File zipFolder = new File(workspaceLocation, name);
        zipFolder.mkdirs();

        try {
            // Unpack jar to a file system
            ZipUtils.extractAll(zipFile, zipFolder);

            // Renamed a project according to rules.xml
            File rules = new File(zipFolder, "rules.xml");
            if (rules.exists()) {
                String rulesName = getProjectName(rules);
                if (rulesName != null && !rulesName.isEmpty()) {
                    // rename project
                    File renamed = new File(workspaceLocation, rulesName);
                    zipFolder.renameTo(renamed);
                    zipFolder = renamed;
                    name = rulesName;
                }
            }

            // Create a deployment project
            ArtefactPathImpl path = new ArtefactPathImpl(name);
            LocalWorkspaceImpl workspace = new LocalWorkspaceImpl(user, workspaceLocation, null, null);
            ADeploymentProject project = new ADeploymentProject(user, new FileRepository(workspaceLocation), path.getStringValue(), null);
            AProject projectToDeploy = new AProject(new FileRepository(workspaceLocation), path.getStringValue());

            // Calculate version
            Repository repository = repositoryFactoryProxy.getRepositoryInstance(config);

            ConfigurationManagerFactory configManagerFactory = ProductionRepositoryFactoryProxy.DEFAULT_CONFIGURATION_MANAGER_FACTORY;
            Map<String, Object> properties = configManagerFactory.getConfigurationManager(config).getProperties();

            // Wait 15 seconds for initializing networking in JGroups.
            Object initializeTimeout = properties.get("timeout.networking.initialize");
            Thread.sleep(initializeTimeout == null ? 15000 : Integer.parseInt(initializeTimeout.toString()));

            // FIXME: Add check on exist deployment
            if (skipExist) {
                log.info("Project [{}] exists. It has been skipped to deploy.", project.getName());
                return;
            }

            // Do deploy
            ArrayList<AProject> projects = new ArrayList<AProject>();
            projects.add(projectToDeploy);
            deployer.deploy(project, projects, user);
            // Wait 10 seconds for finalizing networking in JGroups.
            // + 30 seconds for Infinispan.
            // This time should exceed Infinispan timeouts.
            Object finalizeTimeout = properties.get("timeout.networking.finalize");
            Thread.sleep(finalizeTimeout == null ? 40000 : Integer.parseInt(finalizeTimeout.toString()));
        } finally {
            /* Clean up */
            FileUtils.deleteQuietly(tempDirectory);
            // Close repo
            deployer.destroy();
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
