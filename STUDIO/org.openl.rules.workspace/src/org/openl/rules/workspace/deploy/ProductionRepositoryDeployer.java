package org.openl.rules.workspace.deploy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.zip.ZipInputStream;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.openl.rules.project.resolving.ProjectResolver;
import org.openl.rules.repository.RepositoryInstatiator;
import org.openl.rules.repository.api.ChangesetType;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.FileItem;
import org.openl.rules.repository.api.FolderItem;
import org.openl.rules.repository.api.FolderRepository;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.repository.exceptions.RRepositoryException;
import org.openl.rules.repository.folder.FileChangesFromZip;
import org.openl.util.FileUtils;
import org.openl.util.IOUtils;
import org.openl.util.StringUtils;
import org.openl.util.ZipUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.PropertyResolver;
import org.xml.sax.InputSource;
import org.yaml.snakeyaml.Yaml;

/**
 * This class allows to deploy a zip-based project to a production repository. By default configuration of destination
 * repository is get from "deployer.properties" file.
 *
 * @author Yury Molchan
 */
public class ProductionRepositoryDeployer {
    private final Logger log = LoggerFactory.getLogger(ProductionRepositoryDeployer.class);
    public static final String VERSION_IN_DEPLOYMENT_NAME = ".version-in-deployment-name";
    private static final String DEPLOYMENT_DESCRIPTOR_FILE_NAME = "deployment";
    private PropertyResolver environment;
    private String prefix;

    /**
     *
     * @param environment - environment in which the current application is running
     * @param prefix - name of the production repository configuration which needed
     */
    public ProductionRepositoryDeployer(PropertyResolver environment, String prefix) {
        this.environment = environment;
        this.prefix = prefix;
    }

    /**
     * Deploys a new project to the production repository. If the project exists then it will be skipped to deploy.
     *
     * @param zipFile the project to deploy.
     */
    public void deploy(File zipFile) throws Exception {
        deployInternal(zipFile, true);
    }

    /**
     * Deploys a new or redeploys an existing project to the production repository.
     *
     * @param zipFile the project to deploy.
     */
    public void redeploy(File zipFile) throws Exception {
        deployInternal(zipFile, false);
    }

    public void deployInternal(File zipFile, boolean skipExist) throws Exception {
        Repository deployRepo = null;
        try {
            // Initialize repo
            deployRepo = RepositoryInstatiator.newRepository(prefix, environment);
            String includeVersion = environment.getProperty("repository." + prefix + VERSION_IN_DEPLOYMENT_NAME);
            String deployPath = environment.getProperty("repository." + prefix + ".base.path");
            if (deployPath == null) {
                deployPath = "deploy/"; // Workaround for backward compatibility.
            } else if (!deployPath.isEmpty() && !deployPath.endsWith("/")) {
                deployPath += "/";
            }

            deployInternal(zipFile, deployRepo, skipExist, Boolean.parseBoolean(includeVersion), deployPath);
        } finally {
            // Close repo
            if (deployRepo != null) {
                if (deployRepo instanceof Closeable) {
                    // Close repo connection after validation
                    IOUtils.closeQuietly((Closeable) deployRepo);
                }
            }
        }

    }

    private String getDeploymentName(File zipFolder) {
        File deployment = new File(zipFolder, DEPLOYMENT_DESCRIPTOR_FILE_NAME + ".xml");
        String deploymentName = "openl_rules_" + System.currentTimeMillis();
        if (deployment.exists()) {
            return deploymentName;
        }
        deployment = new File(zipFolder, DEPLOYMENT_DESCRIPTOR_FILE_NAME + ".yaml");
        if (deployment.exists()) {
            try {
                Yaml yaml = new Yaml();
                Map properties = yaml.loadAs(new FileInputStream(deployment), Map.class);
                return Optional.ofNullable(properties.get("name"))
                    .map(Object::toString)
                    .filter(StringUtils::isNotBlank)
                    .orElse(deploymentName);
            } catch (IOException e) {
                log.debug(e.getMessage(), e);
            }
            return deploymentName;
        }
        return null;
    }

    private static List<File> getRulesFolders(File root) {
        ProjectResolver projectResolver = ProjectResolver.getInstance();
        if (projectResolver.isRulesProject(root) != null) {
            return Collections.singletonList(root);
        }
        File[] files = root.listFiles();
        if (files == null) {
            return Collections.emptyList();
        }
        List<File> result = new ArrayList<>();
        for (File entry : files) {
            if (entry.isDirectory() && projectResolver.isRulesProject(entry) != null) {
                result.add(entry);
            }
        }
        return result;
    }

    public void deployInternal(File zipFile,
            Repository deployRepo,
            boolean skipExist,
            boolean includeVersionInDeploymentName,
            String deployPath) throws Exception {

        // Temp folders
        File zipFolder = Files.createTempDirectory("openl").toFile();

        try {
            String name = FileUtils.getBaseName(zipFile.getName());

            // Unpack jar to a file system
            ZipUtils.extractAll(zipFile, zipFolder);

            String deploymentName = getDeploymentName(zipFolder);

            if (deploymentName == null) {
                FileData fileData = createFileData(deployRepo,
                    skipExist,
                    includeVersionInDeploymentName,
                    deployPath,
                    zipFolder,
                    name,
                    null);

                if (fileData == null) {
                    return;
                }
                doDeploy(deployRepo, fileData, zipFile);
            } else {
                List<File> rulesConfigs = getRulesFolders(zipFolder);
                List<FileItem> fileItems = new ArrayList<>();
                for (File f : rulesConfigs) {
                    FileData fileData = createFileData(deployRepo,
                        skipExist,
                        includeVersionInDeploymentName,
                        deployPath,
                        f,
                        name,
                        deploymentName);

                    if (fileData == null) {
                        return;
                    }
                    File modifiedZip = null;
                    try {
                        modifiedZip = File.createTempFile("deployment", ".zip");
                        ZipUtils.archive(f, modifiedZip);
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        IOUtils.copyAndClose(new FileInputStream(modifiedZip), baos);
                        if (!deployRepo.supports().folders()) {
                            fileData.setSize(baos.size());
                        }
                        fileItems.add(new FileItem(fileData, new ByteArrayInputStream(baos.toByteArray())));
                    } finally {
                        FileUtils.deleteQuietly(modifiedZip);
                    }
                }
                if (deployRepo.supports().folders()) {
                    List<FolderItem> folderItems = fileItems.stream().map(fi -> {
                        FileData data = fi.getData();
                        FileChangesFromZip files = new FileChangesFromZip(new ZipInputStream(fi.getStream()),
                            data.getName());
                        return new FolderItem(data, files);
                    }).collect(Collectors.toList());
                    ((FolderRepository) deployRepo).save(folderItems, ChangesetType.FULL);
                } else {
                    deployRepo.save(fileItems);
                }
            }
        } finally {
            /* Clean up */
            FileUtils.deleteQuietly(zipFolder);
        }
    }

    private FileData createFileData(Repository deployRepo,
            boolean skipExist,
            boolean includeVersionInDeploymentName,
            String deployPath,
            File rulesFolder,
            String defaultName,
            String deploymentName) throws RRepositoryException, IOException {
        // Renamed a project according to rules.xml
        String name = defaultName;
        File rules = new File(rulesFolder, "rules.xml");
        if (rules.exists()) {
            String rulesName = getProjectName(rules);
            if (rulesName != null && !rulesName.isEmpty()) {
                name = rulesName;
            }
        }

        int version = 0;
        StringBuilder fileNameBuilder = new StringBuilder(deployPath);
        fileNameBuilder.append(deploymentName == null ? name : deploymentName);
        if (includeVersionInDeploymentName) {
            version = DeployUtils.getNextDeploymentVersion(deployRepo, name, deployPath);
            fileNameBuilder.append(DeployUtils.SEPARATOR).append(version);
        } else {
            File rulesDeploy = new File(rulesFolder, "rules-deploy.xml");
            if (rulesDeploy.exists()) {
                String apiVersion = getApiVersion(rulesDeploy);
                if (apiVersion != null && !apiVersion.isEmpty()) {
                    fileNameBuilder.append(DeployUtils.API_VERSION_SEPARATOR).append(apiVersion);
                }
            }
        }
        fileNameBuilder.append('/');
        if (skipExist) {
            if (includeVersionInDeploymentName) {
                if (version > 1) {
                    log.info("Project [{}] exists. It has been skipped to deploy.", name);
                    return null;
                }
            } else {
                if (!deployRepo.list(fileNameBuilder.toString()).isEmpty()) {
                    return null;
                }
            }
        }
        fileNameBuilder.append(name);
        // Do deploy
        String target = fileNameBuilder.toString();
        FileData dest = new FileData();
        dest.setName(target);
        dest.setAuthor("OpenL_Deployer");
        return dest;
    }

    private void doDeploy(Repository deployRepo, FileData dest, File zipFile) throws IOException {
        if (deployRepo.supports().folders()) {
            try (ZipInputStream stream = new ZipInputStream(new FileInputStream(zipFile))) {
                ((FolderRepository) deployRepo)
                    .save(dest, new FileChangesFromZip(stream, dest.getName()), ChangesetType.FULL);
            }
        } else {
            try (InputStream stream = new FileInputStream(zipFile)) {
                dest.setSize(zipFile.length());
                deployRepo.save(dest, stream);
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
        } catch (FileNotFoundException | XPathExpressionException e) {
            return null;
        }
    }

    private String getApiVersion(File file) {
        try {
            InputSource inputSource = new InputSource(new FileInputStream(file));
            XPathFactory factory = XPathFactory.newInstance();
            XPath xPath = factory.newXPath();
            XPathExpression xPathExpression = xPath.compile("/version");
            return xPathExpression.evaluate(inputSource);
        } catch (FileNotFoundException | XPathExpressionException e) {
            return null;
        }
    }
}
