package org.openl.rules.ruleservice.deployer;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipInputStream;

import org.openl.rules.repository.RepositoryInstatiator;
import org.openl.rules.repository.api.*;
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

    private static final String RULES_XML = "rules.xml";
    private static final String DEFAULT_DEPLOYMENT_NAME = "openl_rules_";
    static final String DEFAULT_AUTHOR_NAME = "OpenL_Deployer";
    private static final String DEPLOYMENT_DESCRIPTOR_FILE_NAME = "deployment";

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final Repository deployRepo;
    private final String deployPath;

    public RulesDeployerService(Repository repository, String deployPath) {
        this.deployRepo = repository;
        this.deployPath = deployPath.isEmpty() || deployPath.endsWith("/") ? deployPath : deployPath + "/";
    }

    /**
     * Initializes repository using target properties
     *
     * @param properties repository settings
     */
    public RulesDeployerService(Properties properties) {
        String deployPath = properties.getProperty("production-repository.base.path");
        this.deployPath = deployPath.isEmpty() || deployPath.endsWith("/") ? deployPath : deployPath + "/";

        Map<String, String> params = new HashMap<>();
        params.put("uri", properties.getProperty("production-repository.uri"));
        params.put("login", properties.getProperty("production-repository.login"));
        params.put("password", properties.getProperty("production-repository.password"));
        // AWS S3 specific
        params.put("bucketName", properties.getProperty("production-repository.bucket-name"));
        params.put("regionName", properties.getProperty("production-repository.region-name"));
        params.put("accessKey", properties.getProperty("production-repository.access-key"));
        params.put("secretKey", properties.getProperty("production-repository.secret-key"));
        // Git specific
        params.put("localRepositoryPath", properties.getProperty("production-repository.local-repository-path"));
        params.put("branch", properties.getProperty("production-repository.branch"));
        params.put("tagPrefix", properties.getProperty("production-repository.tag-prefix"));
        params.put("commentTemplate", properties.getProperty("production-repository.comment-template"));
        params.put("connection-timeout", properties.getProperty("production-repository.connection-timeout"));
        // AWS S3 and Git specific
        params.put("listener-timer-period", properties.getProperty("production-repository.listener-timer-period"));

        this.deployRepo = RepositoryInstatiator.newRepository(properties.getProperty("production-repository.factory"),
            params);
    }

    /**
     * Deploys or redeploys target zip input stream
     *
     * @param name original ZIP file name
     * @param in zip input stream
     * @param overridable if deployment was exist before and overridable is false, it will not be deployed, if true, it
     *            will be overridden.
     */
    public void deploy(String name, InputStream in, boolean overridable) throws Exception {
        deployInternal(name, in, overridable);
    }

    public void deploy(InputStream in, boolean overridable) throws Exception {
        deployInternal(null, in, overridable);
    }

    /**
     * Read a file by the given path name.
     *
     * @param serviceName the path name of the file to read.
     * @return the file descriptor or null if the file is absent.
     * @throws IOException if not possible to read the file.
     */
    public FileItem read(String serviceName) throws IOException {
        return deployRepo.read(serviceName);
    }

    /**
     * Delete a file or mark it as deleted.
     *
     * @param serviceName the path name of the file to delete.
     * @return true if file has been deleted successfully or false if the file is absent or cannot be deleted.
     */
    public boolean delete(String serviceName) throws IOException {
        FileData fileDate = deployRepo.check(serviceName);
        return deployRepo.delete(fileDate);
    }

    private void deployInternal(String originalName, InputStream in, boolean overridable) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        IOUtils.copyAndClose(in, baos);

        if (baos.size() == 0) {
            throw new RuntimeException("Zip file input stream is empty");
        }

        Map<String, byte[]> zipEntries = DeploymentUtils.unzip(new ByteArrayInputStream(baos.toByteArray()));

        String deploymentName = getDeploymentName(zipEntries);
        String name = originalName != null ? originalName : DEFAULT_DEPLOYMENT_NAME + System.currentTimeMillis();
        if (deploymentName == null) {
            FileData dest = createFileData(zipEntries, null, name, overridable);
            if (dest != null) {
                doDeploy(dest, baos.size(), new ByteArrayInputStream(baos.toByteArray()));
            }
        } else {
            List<FileItem> fileItems = splitMultipleDeployment(zipEntries, deploymentName, name, overridable);

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
    }

    private List<FileItem> splitMultipleDeployment(Map<String, byte[]> zipEntries,
            String deploymentName,
            String name,
            boolean overridable) throws Exception {
        Set<String> projectFolders = new HashSet<>();
        String rulesXml = "/" + RULES_XML;
        for (String fileName : zipEntries.keySet()) {
            int last = fileName.lastIndexOf(rulesXml);
            if (last > 0) {
                String projectFolder = fileName.substring(0, last + 1);
                projectFolders.add(projectFolder);
            }
        }
        if (projectFolders.isEmpty()) {
            return Collections.emptyList();
        }
        List<FileItem> fileItems = new ArrayList<>();
        for (String projectFolder : projectFolders) {
            Map<String, byte[]> newProjectEntries = new HashMap<>();
            for (Map.Entry<String, byte[]> entry : zipEntries.entrySet()) {
                String originalPath = entry.getKey();
                if (originalPath.startsWith(projectFolder)) {
                    String newPath = originalPath.substring(projectFolder.length());
                    newProjectEntries.put(newPath, entry.getValue());
                }
            }
            if (!newProjectEntries.isEmpty()) {
                FileData dest = createFileData(newProjectEntries, deploymentName, name, overridable);
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

    private String getDeploymentName(Map<String, byte[]> zipEntries) {
        String deploymentName = DEFAULT_DEPLOYMENT_NAME + System.currentTimeMillis();
        if (zipEntries.get(DEPLOYMENT_DESCRIPTOR_FILE_NAME + ".xml") != null) {
            return deploymentName;
        } else {
            byte[] bytes = zipEntries.get(DEPLOYMENT_DESCRIPTOR_FILE_NAME + ".yaml");
            if (bytes == null) {
                return null;
            }
            try (InputStream fileStream = new ByteArrayInputStream(bytes)) {
                Yaml yaml = new Yaml();
                Map properties = yaml.loadAs(fileStream, Map.class);
                return Optional.ofNullable(properties.get("name"))
                    .map(Object::toString)
                    .filter(StringUtils::isNotBlank)
                    .orElse(deploymentName);
            } catch (IOException e) {
                log.debug(e.getMessage(), e);
                return deploymentName;
            }
        }
    }

    private FileData createFileData(Map<String, byte[]> zipEntries,
            String defaultDeploymentName,
            String defaultName,
            boolean overridable) throws Exception {

        String projectName = readProjectName(zipEntries.get(RULES_XML), defaultName);
        String apiVersion = readApiVersion(zipEntries.get("rules-deploy.xml"));

        String deploymentName = defaultDeploymentName == null ? projectName : defaultDeploymentName;
        if (apiVersion != null && !apiVersion.isEmpty()) {
            deploymentName += DeploymentUtils.API_VERSION_SEPARATOR + apiVersion;
        }

        if (!overridable && isRulesDeployed(deploymentName)) {
            log.info("Module '{}' is skipped for deploy because it has been already deployed.", deploymentName);
            return null;
        }
        FileData dest = new FileData();
        dest.setName(deployPath + deploymentName + '/' + projectName);
        dest.setAuthor(DEFAULT_AUTHOR_NAME);
        return dest;
    }

    private String readProjectName(byte[] bytes, String defaultName) {
        if (bytes == null) {
            return null;
        }
        String name = DeploymentUtils.getProjectName(new ByteArrayInputStream(bytes));
        return name == null || name.isEmpty() ? defaultName : name;
    }

    private String readApiVersion(byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        return DeploymentUtils.getApiVersion(new ByteArrayInputStream(bytes));
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
        if (deployRepo instanceof Closeable) {
            // Close repo connection after validation
            IOUtils.closeQuietly((Closeable) deployRepo);
        }
    }
}
