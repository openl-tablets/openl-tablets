package org.openl.rules.activiti.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.activiti.engine.RepositoryService;
import org.activiti.engine.impl.context.Context;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.openl.rules.activiti.ResourceNotFoundException;
import org.openl.rules.project.IRulesDeploySerializer;
import org.openl.rules.project.model.RulesDeploy;
import org.openl.rules.project.xml.XmlRulesDeploySerializer;

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
            throw new ResourceNotFoundException(String.format("No resource found with name \"%s\"!", resource));
        }

        final String tmpOpenLProjectFileName = UUID.randomUUID().toString() + "-openl-resource";
        final File workspaceFolder = File.createTempFile(tmpOpenLProjectFileName, "");
        workspaceFolder.delete();
        workspaceFolder.mkdir();

        if (resource.endsWith(".zip")) {
            ResourceUtils.unzip(inputStream, workspaceFolder);
        } else {
            FileUtils.deleteDirectory(workspaceFolder);
            workspaceFolder.mkdir();
            File resourceFile = new File(workspaceFolder, resource);
            ResourceUtils.copy(inputStream, resourceFile);
        }

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                FileUtils.deleteQuietly(workspaceFolder);
            }
        });
        return workspaceFolder;
    }

    public static void copy(InputStream inputStream, File outputFile) throws IOException {
        FileOutputStream fos = new FileOutputStream(outputFile);
        try {
            IOUtils.copy(inputStream, fos);
        } finally {
            IOUtils.closeQuietly(fos);
        }
    }

    public static void unzip(InputStream inputStream, File outputDir) throws IOException {
        ZipInputStream zis = null;
        try {
            zis = new ZipInputStream(inputStream);
            ZipEntry entry;

            while ((entry = zis.getNextEntry()) != null) {
                // Create a file on HDD in the destinationPath directory
                // destinationPath is a "root" folder, where you want to
                // extract
                // your ZIP file
                File entryFile = new File(outputDir, entry.getName());
                if (entry.isDirectory()) {
                    if (entryFile.exists()) {
                        // logger.log(Level.WARNING,
                        // "Directory {0} already exists!", entryFile);
                    } else {
                        entryFile.mkdirs();
                    }

                } else {

                    // Make sure all folders exists (they should, but the
                    // safer,
                    // the better ;-))
                    if (entryFile.getParentFile() != null && !entryFile.getParentFile().exists()) {
                        entryFile.getParentFile().mkdirs();
                    }

                    // Create file on disk...
                    if (!entryFile.exists()) {
                        entryFile.createNewFile();
                    }

                    // and rewrite data from stream
                    OutputStream os = null;
                    try {
                        os = new FileOutputStream(entryFile);
                        IOUtils.copy(zis, os);
                    } finally {
                        IOUtils.closeQuietly(os);
                    }
                }
            }
        } finally {
            IOUtils.closeQuietly(zis);
        }
    }
}
