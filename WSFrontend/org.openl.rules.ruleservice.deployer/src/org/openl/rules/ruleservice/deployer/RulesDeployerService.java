package org.openl.rules.ruleservice.deployer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.openl.rules.repository.RepositoryInstatiator;
import org.openl.rules.repository.api.ChangesetType;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.FileItem;
import org.openl.rules.repository.api.FolderItem;
import org.openl.rules.repository.api.FolderRepository;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.repository.folder.FileChangesFromZip;
import org.openl.util.IOUtils;
import org.openl.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

/**
 * This class allows to deploy a zip-based project to a production repository.
 *
 * @author Vladyslav Pikus
 */
public class RulesDeployerService implements Closeable {

    private static final Logger LOG = LoggerFactory.getLogger(RulesDeployerService.class);

    private static final Set<String> DEPLOY_DESCRIPTOR_FILES = Stream.of(DeploymentDescriptor.values())
            .map(DeploymentDescriptor::getFileName)
            .collect(Collectors.toSet());

    private static final String RULES_XML = "rules.xml";
    private static final String RULES_DEPLOY_XML = "rules-deploy.xml";
    private static final String DEFAULT_DEPLOYMENT_NAME = "openl_rules_";
    static final String DEFAULT_AUTHOR_NAME = "OpenL_Deployer";

    private final Repository deployRepo;
    private final String deployPath;

    public RulesDeployerService(Repository repository, String deployPath) {
        this.deployRepo = repository;
        if (deployRepo.supports().isLocal()) {
            // NOTE deployment path isn't required for LocalRepository. It must be specified within URI
            this.deployPath = "";
        } else {
            this.deployPath = deployPath.isEmpty() || deployPath.endsWith("/") ? deployPath : deployPath + "/";
        }
    }

    /**
     * Initializes repository using target properties
     *
     * @param properties repository settings
     */
    public RulesDeployerService(Properties properties) {
        this.deployRepo = RepositoryInstatiator.newRepository("production-repository", properties::getProperty);
        if (deployRepo.supports().isLocal()) {
            // NOTE deployment path isn't required for LocalRepository. It must be specified within URI
            this.deployPath = "";
        } else {
            String deployPath = properties.getProperty("production-repository.base.path");
            this.deployPath = deployPath.isEmpty() || deployPath.endsWith("/") ? deployPath : deployPath + "/";
        }
    }

    /**
     * Deploys or redeploys target zip input stream
     *
     * @param name original ZIP file name
     * @param in zip input stream
     * @param ignoreIfExists if deployment was exist before and overridable is false, it will not be deployed, if true, it
     *            will be overridden.
     */
    public void deploy(String name, InputStream in, boolean ignoreIfExists) throws IOException, RulesDeployInputException {
        deployInternal(name, in, ignoreIfExists);
    }

    public void deploy(InputStream in, boolean ignoreIfExists) throws IOException, RulesDeployInputException {
        deployInternal(null, in, ignoreIfExists);
    }

    /**
     * Read a service by the given path name.
     *
     * @param deployPath deployPath of the service to read.
     * @return the InputStream containing project archive.
     * @throws IOException if not possible to read the file.
     */
    public InputStream read(String deployPath) throws IOException {
        if (deployRepo.supports().folders()) {
            deployPath = deployPath + "/";
            List<FileData> files = deployRepo.list(deployPath);
            ByteArrayOutputStream fos = new ByteArrayOutputStream();
            ZipOutputStream zipOut = new ZipOutputStream(fos);
            for (FileData fileData : files) {
                FileItem fileItem = deployRepo.read(fileData.getName());
                ZipEntry zipEntry = new ZipEntry(fileItem.getData().getName().replace(deployPath, ""));
                zipOut.putNextEntry(zipEntry);
                InputStream stream = fileItem.getStream();
                byte[] bytes = new byte[1024];
                int length;
                while ((length = stream.read(bytes)) >= 0) {
                    zipOut.write(bytes, 0, length);
                }
                stream.close();
            }
            zipOut.close();
            fos.close();
            return new ByteArrayInputStream(fos.toByteArray());
        } else {
            return deployRepo.read(deployPath).getStream();
        }
    }

    /**
     * Delete a file or mark it as deleted.
     *
     * @param deployPath deployPath of the file to delete.
     * @return true if file has been deleted successfully or false if the file is absent or cannot be deleted.
     */
    public boolean delete(String deployPath) throws IOException {
        FileData fileDate = deployRepo.check(deployPath);
        if (deployRepo.delete(fileDate)) {
            deleteDeploymentDescriptors(deployPath);
            return true;
        }
        return false;
    }

    private void deleteDeploymentDescriptors(String deployPath) throws IOException {
        if (deployRepo.supports().folders()) {
            if (deployPath.charAt(0) == '/') {
                deployPath = deployPath.substring(1);
            }
            final String deploymentName = deployPath.split("/")[0];
            if (((FolderRepository) deployRepo).listFolders(deploymentName).isEmpty()) {
                for (String deployDescriptorFile : DEPLOY_DESCRIPTOR_FILES) {
                    FileData fd = deployRepo.check(deploymentName + "/" + deployDescriptorFile);
                    if (fd != null) {
                        deployRepo.delete(fd);
                    }
                }
            }
        }
    }

    private void deployInternal(String originalName, InputStream in, boolean ignoreIfExists) throws IOException,
                                                                                          RulesDeployInputException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        IOUtils.copyAndClose(in, baos);

        Map<String, byte[]> zipEntries = DeploymentUtils.unzip(new ByteArrayInputStream(baos.toByteArray()));

        if (baos.size() == 0 || zipEntries.size() == 0) {
            throw new RulesDeployInputException("Cannot create a project from the given file. Zip file is empty.");
        }

        if (!hasDeploymentDescriptor(zipEntries)) {
            String projectName = Optional.ofNullable(zipEntries.get(RULES_XML))
                    .map(DeploymentUtils::getProjectName)
                    .filter(StringUtils::isNotBlank)
                    .orElse(null);
            if (projectName == null) {
                projectName = StringUtils.isNotBlank(originalName) ? originalName : randomDeploymentName();
            }
            FileData dest = createFileData(zipEntries, projectName, projectName, ignoreIfExists);
            if (dest != null) {
                doDeploy(dest, baos.size(), new ByteArrayInputStream(baos.toByteArray()));
            }
        } else {
            if (deployRepo.supports().folders()) {
                String deploymentName = getDeploymentName(zipEntries);
                if (StringUtils.isBlank(deploymentName)) {
                    deploymentName = StringUtils.isNotBlank(originalName)
                            ? originalName : randomDeploymentName();
                }
                if (!ignoreIfExists && isRulesDeployed(deploymentName)) {
                    LOG.info("Module '{}' is skipped for deploy because it has been already deployed.", deploymentName);
                    return;
                }
                FileData dest = new FileData();
                dest.setName(deployPath + deploymentName);
                dest.setAuthor(DEFAULT_AUTHOR_NAME);
                dest.setSize(baos.size());
                FileChangesFromZip changes = new FileChangesFromZip(new ZipInputStream(new ByteArrayInputStream(baos.toByteArray())), dest.getName());
                ((FolderRepository) deployRepo).save(Collections.singletonList(new FolderItem(dest, changes)), ChangesetType.FULL);
            } else {
                //split zip to single-project deployment if repository doesn't support folders
                List<FileItem> fileItems = splitMultipleDeployment(zipEntries, originalName, ignoreIfExists);
                deployRepo.save(fileItems);
            }
        }
    }

    private static String randomDeploymentName() {
        return DEFAULT_DEPLOYMENT_NAME + System.currentTimeMillis();
    }

    private List<FileItem> splitMultipleDeployment(Map<String, byte[]> zipEntries,
            String defaultDeploymentName,
            boolean ignoreIfExists) throws IOException {
        Set<String> projectFolders = new HashSet<>();
        for (String fileName : zipEntries.keySet()) {
            int idx = fileName.indexOf('/');
            if (idx > 0) {
                String projectFolder = fileName.substring(0, idx);
                projectFolders.add(projectFolder);
            }
        }
        if (projectFolders.isEmpty()) {
            return Collections.emptyList();
        }
        List<FileItem> fileItems = new ArrayList<>();
        String deploymentName = getDeploymentName(zipEntries);
        if (StringUtils.isBlank(deploymentName)) {
            deploymentName = StringUtils.isNotBlank(defaultDeploymentName)
                    ? defaultDeploymentName : randomDeploymentName();
        }
        for (String projectFolder : projectFolders) {
            Map<String, byte[]> newProjectEntries = new HashMap<>();
            for (Map.Entry<String, byte[]> entry : zipEntries.entrySet()) {
                String originalPath = entry.getKey();
                if (originalPath.startsWith(projectFolder + "/")) {
                    String newPath = originalPath.substring(projectFolder.length() + 1);
                    newProjectEntries.put(newPath, entry.getValue());
                }
            }
            if (!newProjectEntries.isEmpty()) {
                FileData dest = createFileData(newProjectEntries, deploymentName, projectFolder, ignoreIfExists);
                if (dest == null) {
                    return Collections.emptyList();
                }
                ByteArrayOutputStream zipbaos = DeploymentUtils.archiveAsZip(newProjectEntries);
                if (!deployRepo.supports().folders()) {
                    dest.setSize(zipbaos.size());
                }
                fileItems.add(new FileItem(dest, new ByteArrayInputStream(zipbaos.toByteArray())));
            }
        }
        if (fileItems.isEmpty()) {
            throw new RuntimeException("Invalid deployment structure! Cannot detect projects.");
        }
        return fileItems;
    }

    private static boolean hasDeploymentDescriptor(Map<String, byte[]> zipEntries) {
        return zipEntries.get(DeploymentDescriptor.XML.getFileName()) != null
                || zipEntries.get(DeploymentDescriptor.YAML.getFileName()) != null;
    }

    private static String getDeploymentName(Map<String, byte[]> zipEntries) {
        if (zipEntries.get(DeploymentDescriptor.XML.getFileName()) != null) {
            return null;
        } else {
            byte[] bytes = zipEntries.get(DeploymentDescriptor.YAML.getFileName());
            try (InputStream fileStream = new ByteArrayInputStream(bytes)) {
                Yaml yaml = new Yaml();
                return Optional.ofNullable(yaml.loadAs(fileStream, Map.class))
                    .map(prop -> prop.get("name"))
                    .map(Object::toString)
                    .filter(StringUtils::isNotBlank)
                    .orElse(null);
            } catch (IOException e) {
                LOG.debug(e.getMessage(), e);
                return null;
            }
        }
    }

    private FileData createFileData(Map<String, byte[]> zipEntries,
            String deploymentName,
            String projectName,
            boolean ignoreIfExists) throws IOException {

        String apiVersion = Optional.ofNullable(zipEntries.get(RULES_DEPLOY_XML))
                .map(DeploymentUtils::getApiVersion)
                .filter(StringUtils::isNotBlank)
                .orElse(null);
        if (apiVersion != null) {
            deploymentName += DeploymentUtils.API_VERSION_SEPARATOR + apiVersion;
        }

        if (!ignoreIfExists && isRulesDeployed(deploymentName)) {
            LOG.info("Module '{}' is skipped for deploy because it has been already deployed.", deploymentName);
            return null;
        }
        FileData dest = new FileData();
        String name = deployPath + deploymentName;
        dest.setName(name + '/' + projectName);
        dest.setAuthor(DEFAULT_AUTHOR_NAME);
        return dest;
    }

    private void doDeploy(FileData dest, Integer contentSize, InputStream inputStream) throws IOException {
        if (deployRepo.supports().folders()) {
            ((FolderRepository) deployRepo).save(dest,
                new FileChangesFromZip(new ZipInputStream(inputStream), dest.getName()),
                ChangesetType.FULL);
        } else {
            dest.setSize(contentSize);
            deployRepo.save(dest, inputStream);
        }
    }

    private boolean isRulesDeployed(String deploymentName) throws IOException {
        List<FileData> deployments = deployRepo.list(deployPath + deploymentName + "/");
        return !deployments.isEmpty();
    }

    @Override
    public void close() {
        // Close repo connection after validation
        IOUtils.closeQuietly(deployRepo);
    }
}
