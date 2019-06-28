package org.openl.rules.activiti.util;

import java.io.*;
import java.nio.file.Files;

import org.activiti.engine.RepositoryService;
import org.activiti.engine.impl.context.Context;
import org.openl.rules.activiti.ResourceNotFoundException;
import org.openl.rules.project.IRulesDeploySerializer;
import org.openl.rules.project.model.RulesDeploy;
import org.openl.rules.project.xml.XmlRulesDeploySerializer;
import org.openl.util.FileUtils;
import org.openl.util.IOUtils;
import org.openl.util.ZipUtils;

public final class ResourceUtils {
    public static final String RULES_DEPLOY_XML = "rules-deploy.xml";

    private ResourceUtils() {
    }

    private static IRulesDeploySerializer rulesDeploySerializer = new XmlRulesDeploySerializer();

    public static RulesDeploy readRulesDeploy(File openlProjectFolder) throws IOException {
        File rulesDeployXmlFile = new File(openlProjectFolder, RULES_DEPLOY_XML);
        if (rulesDeployXmlFile.exists() && rulesDeployXmlFile.isFile()) {
            return rulesDeploySerializer.deserialize(new FileInputStream(rulesDeployXmlFile));
        }
        return null;
    }

    public static File prepareDeploymentOpenLResource(String deploymentId, String resource) throws IOException {
        RepositoryService repositoryService = Context.getProcessEngineConfiguration().getRepositoryService();
        InputStream inputStream = repositoryService.getResourceAsStream(deploymentId, resource);
        if (inputStream == null) {
            throw new ResourceNotFoundException(String.format("No resource found with name '%s'!", resource));
        }

        final File workspaceFolder = Files.createTempDirectory("openl").toFile();

        if (resource.endsWith(".zip")) {
            // Unzip
            ZipUtils.extractAll(inputStream, workspaceFolder);
        } else {
            // Copy
            File resourceFile = new File(workspaceFolder, resource);
            FileOutputStream fos = new FileOutputStream(resourceFile);
            IOUtils.copyAndClose(inputStream, fos);
        }

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                FileUtils.deleteQuietly(workspaceFolder);
            }
        });
        return workspaceFolder;
    }
}
