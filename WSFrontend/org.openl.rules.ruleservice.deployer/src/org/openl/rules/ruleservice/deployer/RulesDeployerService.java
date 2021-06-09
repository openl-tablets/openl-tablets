package org.openl.rules.ruleservice.deployer;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
import org.openl.util.FileSignatureHelper;
import org.openl.util.FileTypeHelper;
import org.openl.util.FileUtils;
import org.openl.util.IOUtils;
import org.openl.util.StringUtils;
import org.openl.util.ZipUtils;
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

    private static final String RULES_XML = "rules.xml";
    private static final String RULES_DEPLOY_XML = "rules-deploy.xml";
    private static final String DEFAULT_DEPLOYMENT_NAME = "openl_rules_";
    static final String DEFAULT_AUTHOR_NAME = "OpenL_Deployer";

    private final Repository deployRepo;
    private final String baseDeployPath;

    public RulesDeployerService(Repository repository, String baseDeployPath) {
        this.deployRepo = repository;
        if (deployRepo.supports().isLocal()) {
            // NOTE deployment path isn't required for LocalRepository. It must be specified within URI
            this.baseDeployPath = "";
        } else {
            this.baseDeployPath = baseDeployPath.isEmpty() || baseDeployPath.endsWith("/") ? baseDeployPath
                                                                                           : baseDeployPath + "/";
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
            this.baseDeployPath = "";
        } else {
            String deployPath = properties.getProperty("production-repository.base.path");
            this.baseDeployPath = deployPath.isEmpty() || deployPath.endsWith("/") ? deployPath : deployPath + "/";
        }
    }

    /**
     * Deploys or redeploys target zip input stream
     *
     * @param name original ZIP file name
     * @param in zip input stream
     * @param ignoreIfExists if deployment was exist before and overridable is false, it will not be deployed, if true,
     *            it will be overridden.
     */
    public void deploy(String name, InputStream in, boolean ignoreIfExists) throws IOException,
                                                                            RulesDeployInputException {
        Path archiveTmp = Files.createTempFile(StringUtils.isBlank(name) ? DEFAULT_DEPLOYMENT_NAME : name, ".zip");
        try {
            IOUtils.copyAndClose(in, Files.newOutputStream(archiveTmp));
            deployInternal(archiveTmp, name, ignoreIfExists);
        } finally {
            deleteQuietly(archiveTmp);
        }
    }

    public void deploy(InputStream in, boolean ignoreIfExists) throws IOException, RulesDeployInputException {
        Path archiveTmp = Files.createTempFile(DEFAULT_DEPLOYMENT_NAME, ".zip");
        try {
            IOUtils.copyAndClose(in, Files.newOutputStream(archiveTmp));
            deployInternal(archiveTmp, null, ignoreIfExists);
        } finally {
            deleteQuietly(archiveTmp);
        }
    }

    public void deploy(File file, boolean ignoreIfExists) throws IOException, RulesDeployInputException {
        deployInternal(file.toPath(), FileUtils.getBaseName(file.getName()), ignoreIfExists);
    }

    /**
     * Read a service by the given path name.
     *
     * @param deployPath deployPath of the service to read.
     * @throws IOException if not possible to read the file.
     */
    public void read(String deployPath, Set<String> projectsPath, OutputStream output) throws IOException {
        if (deployRepo.supports().folders()) {
            final String fullDeployPath = baseDeployPath + deployPath;
            try {
                if (Optional.ofNullable(deployRepo.check(fullDeployPath))
                        .map(FileData::getSize)
                        .filter(size -> size > FileData.UNDEFINED_SIZE)
                        .isPresent()) {
                    FileItem archive = deployRepo.read(fullDeployPath);
                    if (archive != null) {
                        IOUtils.copyAndClose(archive.getStream(), output);
                        return;
                    }
                }
            } catch (IOException ignored) {
                // OK
            }
            final boolean isDeployment = hasDeploymentDescriptor(fullDeployPath);
            final boolean isMultiProject = isDeployment || ((FolderRepository) deployRepo).listFolders(fullDeployPath)
                .size() > 1;

            final String basePath = (isMultiProject ? fullDeployPath : baseDeployPath + projectsPath.iterator().next()) + "/";
            List<FileData> files = deployRepo.list(basePath);
            try (ZipOutputStream target = new ZipOutputStream(output)) {
                for (FileData fileData : files) {
                    try (FileItem fileItem = deployRepo.read(fileData.getName())) {
                        ZipEntry targetEntry = new ZipEntry(
                            fileItem.getData().getName().substring(basePath.length()));
                        target.putNextEntry(targetEntry);
                        IOUtils.copy(fileItem.getStream(), target);
                    }
                }
            }
        } else {
            if (projectsPath.size() == 1) {
                IOUtils.copyAndClose(deployRepo.read(baseDeployPath + projectsPath.iterator().next()).getStream(),
                    output);
                return;
            }
            try (ZipOutputStream target = new ZipOutputStream(output)) {
                for (String projectPath : projectsPath) {
                    final String projectFolder = projectPath.substring(deployPath.length() + 1) + "/";
                    final String fullDeployPath = baseDeployPath + projectPath;
                    try (ZipInputStream source = new ZipInputStream(deployRepo.read(fullDeployPath).getStream())) {
                        ZipEntry sourceEntry;
                        while ((sourceEntry = source.getNextEntry()) != null) {
                            ZipEntry targetEntry = new ZipEntry(projectFolder + sourceEntry.getName());
                            target.putNextEntry(targetEntry);
                            if (!sourceEntry.isDirectory()) {
                                IOUtils.copy(source, target);
                            }
                        }
                    }
                }
            }
        }
    }

    private boolean hasDeploymentDescriptor(String deployPath) throws IOException {
        return deployRepo.check(deployPath + "/" + DeploymentDescriptor.YAML.getFileName()) != null || deployRepo
            .check(deployPath + "/" + DeploymentDescriptor.XML.getFileName()) != null;
    }

    /**
     * Delete a file or mark it as deleted.
     *
     * @param deployPath deployPath of the file to delete.
     * @return true if file has been deleted successfully or false if the file is absent or cannot be deleted.
     */
    public boolean delete(String deployPath, Set<String> projectsPath) throws IOException {
        if (deployRepo.supports().folders()) {
            FileData data = new FileData();
            data.setName(baseDeployPath + deployPath);
            data.setAuthor(DEFAULT_AUTHOR_NAME);
            data.setComment("Delete deployment.");
            return deployRepo.deleteHistory(data);
        } else {
            List<FileData> toDelete = projectsPath.stream().map(name -> baseDeployPath + name).map(name -> {
                FileData data = new FileData();
                data.setName(name);
                data.setAuthor(DEFAULT_AUTHOR_NAME);
                data.setComment("Delete deployment.");
                return data;
            }).collect(Collectors.toList());
            return deployRepo.delete(toDelete);
        }
    }

    private void deployInternal(Path pathToArchive,
            String originalName,
            boolean ignoreIfExists) throws IOException, RulesDeployInputException {
        validateSignature(pathToArchive);
        if (originalName != null) {
            // For some reason Java doesn't allow trailing whitespace in folder names
            originalName = originalName.trim();
        }
        try (FileSystem fs = FileSystems.newFileSystem(ZipUtils.toJarURI(pathToArchive), Collections.emptyMap())) {
            final Path root = fs.getPath("/");
            if (isRulesProject(root)) {
                deployRegularProject(pathToArchive, originalName, ignoreIfExists, root);
            } else {
                String deploymentName = Stream
                    .of(DeploymentDescriptor.XML.getFileName(), DeploymentDescriptor.YAML.getFileName())
                    .map(root::resolve)
                    .filter(Files::exists)
                    .filter(Files::isRegularFile)
                    .map(RulesDeployerService::getDeploymentName)
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(null);
                if (StringUtils.isBlank(deploymentName)) {
                    deploymentName = StringUtils.isNotBlank(originalName) ? originalName : randomDeploymentName();
                }
                deployMultiProject(pathToArchive, ignoreIfExists, root, deploymentName);
            }
        }
    }

    private void deployMultiProject(Path pathToArchive,
            boolean ignoreIfExists,
            Path root,
            String deploymentName) throws IOException {

        if (deployRepo.supports().folders()) {
            if (!ignoreIfExists && isRulesDeployed(deploymentName)) {
                LOG.info("Module '{}' is skipped for deploy because it has been already deployed.", deploymentName);
                return;
            }
            BasicFileAttributes attrs = Files.readAttributes(pathToArchive, BasicFileAttributes.class);
            FileData dest = new FileData();
            dest.setName(baseDeployPath + deploymentName);
            dest.setAuthor(DEFAULT_AUTHOR_NAME);
            dest.setSize(attrs.size());
            try (FileChangesFromFolder changes = new FileChangesFromFolder(root, dest.getName())) {
                ((FolderRepository) deployRepo).save(Collections.singletonList(new FolderItem(dest, changes)),
                    ChangesetType.FULL);
            }
        } else {
            // split zip to single-project deployment if repository doesn't support folders
            final List<Path> folders;
            try (Stream<Path> stream = Files.walk(root, 1)) {
                folders = stream.filter(path -> !root.equals(path)).filter(Files::isDirectory).map(folder -> {
                    String s = folder.toString();
                    if (s.endsWith("/")) {
                        return root.resolve(s.substring(0, s.length() - 1));
                    }
                    return folder;
                }).filter(RulesDeployerService::isRulesProject).collect(Collectors.toList());
            }
            List<Path> tmpArchives = new ArrayList<>();
            List<FileItem> fileItems = new ArrayList<>();
            try {
                for (Path folder : folders) {
                    String folderName = folder.getFileName().toString();
                    Optional<FileData> fileData = createFileData(folder, deploymentName, folderName, ignoreIfExists);
                    if (!fileData.isPresent()) {
                        continue;
                    }
                    Path tmp = Files.createTempFile(folderName, ".zip");
                    tmpArchives.add(tmp);
                    try (ZipOutputStream target = new ZipOutputStream(Files.newOutputStream(tmp))) {
                        Files.walkFileTree(folder, new SimpleFileVisitor<Path>() {
                            @Override
                            public FileVisitResult visitFile(Path p, BasicFileAttributes attr) throws IOException {
                                if (!attr.isRegularFile()) {
                                    return FileVisitResult.CONTINUE;
                                }
                                ZipEntry targetEntry = new ZipEntry(folder.relativize(p).toString());
                                target.putNextEntry(targetEntry);
                                try (InputStream source = Files.newInputStream(p)) {
                                    IOUtils.copy(source, target);
                                }
                                return FileVisitResult.CONTINUE;
                            }
                        });
                    }
                    BasicFileAttributes attrs = Files.readAttributes(tmp, BasicFileAttributes.class);
                    FileData dest = fileData.get();
                    dest.setSize(attrs.size());
                    fileItems.add(new FileItem(dest, Files.newInputStream(tmp)));
                }
                if (!fileItems.isEmpty()) {
                    deployRepo.save(fileItems);
                }
            } finally {
                fileItems.stream().map(FileItem::getStream).forEach(IOUtils::closeQuietly);
                tmpArchives.forEach(RulesDeployerService::deleteQuietly);
            }
        }
    }

    private void deployRegularProject(Path pathToArchive,
            String originalName,
            boolean ignoreIfExists,
            Path root) throws IOException {

        String projectName = getProjectDescriptor(root).map(f -> {
            try (InputStream in = Files.newInputStream(f)) {
                return DeploymentUtils.getProjectName(in);
            } catch (IOException e) {
                LOG.debug(e.getMessage(), e);
                return null;
            }
        }).orElse(null);
        if (projectName == null) {
            projectName = StringUtils.isNotBlank(originalName) ? originalName : randomDeploymentName();
        }
        Optional<FileData> fileData = createFileData(root, projectName, projectName, ignoreIfExists);
        if (fileData.isPresent()) {
            FileData dest = fileData.get();
            if (deployRepo.supports().folders()) {
                try (FileChangesFromFolder changes = new FileChangesFromFolder(root, dest.getName())) {
                    ((FolderRepository) deployRepo).save(dest, changes, ChangesetType.FULL);
                }
            } else {
                BasicFileAttributes attrs = Files.readAttributes(pathToArchive, BasicFileAttributes.class);
                dest.setSize(attrs.size());
                try (InputStream inputStream = Files.newInputStream(pathToArchive)) {
                    deployRepo.save(dest, inputStream);
                }
            }
        }
    }

    private static String randomDeploymentName() {
        return DEFAULT_DEPLOYMENT_NAME + System.currentTimeMillis();
    }

    private static String getDeploymentName(Path deploymentDescriptor) {
        if (DeploymentDescriptor.XML.getFileName().equals(deploymentDescriptor.getFileName().toString())) {
            return null;
        } else {
            try (InputStream fileStream = Files.newInputStream(deploymentDescriptor)) {
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

    private Optional<FileData> createFileData(Path root,
            String deploymentName,
            String projectName,
            boolean ignoreIfExists) throws IOException {
        Optional<String> apiVersion = Optional.of(RULES_DEPLOY_XML)
            .map(root::resolve)
            .filter(Files::isRegularFile)
            .map(f -> {
                try (InputStream in = Files.newInputStream(f)) {
                    return DeploymentUtils.getApiVersion(in);
                } catch (IOException e) {
                    LOG.debug(e.getMessage(), e);
                    return null;
                }
            })
            .filter(StringUtils::isNotBlank);

        if (apiVersion.isPresent()) {
            deploymentName += DeploymentUtils.API_VERSION_SEPARATOR + apiVersion.get();
        }

        if (!ignoreIfExists && isRulesDeployed(deploymentName)) {
            LOG.info("Module '{}' is skipped for deploy because it has been already deployed.", deploymentName);
            return Optional.empty();
        }
        FileData dest = new FileData();
        String name = baseDeployPath + deploymentName;
        dest.setName(name + '/' + projectName);
        dest.setAuthor(DEFAULT_AUTHOR_NAME);
        return Optional.of(dest);
    }

    private boolean isRulesDeployed(String deploymentName) throws IOException {
        List<FileData> deployments = deployRepo.list(baseDeployPath + deploymentName + "/");
        return !deployments.isEmpty();
    }

    private static void validateSignature(Path path) throws RulesDeployInputException {
        if (!Files.isRegularFile(path)) {
            throw new RulesDeployInputException("Provided file is not an archive!");
        }
        int sign = readSignature(path);
        if (!FileSignatureHelper.isArchiveSign(sign)) {
            throw new RulesDeployInputException("Provided file is not an archive!");
        }
        if (FileSignatureHelper.isEmptyArchive(sign)) {
            throw new RulesDeployInputException("Cannot create a project from the given file. Zip file is empty.");
        }
    }

    private static int readSignature(Path path) {
        try (RandomAccessFile raf = new RandomAccessFile(path.toFile(), "r")) {
            return raf.readInt();
        } catch (IOException ignored) {
            return -1;
        }
    }

    private static void deleteQuietly(Path path) {
        try {
            Files.delete(path);
        } catch (IOException e) {
            LOG.debug(e.getMessage(), e);
        }
    }

    private static boolean isRulesProject(Path root) {
        if (getProjectDescriptor(root).isPresent()) {
            return true;
        }
        try (Stream<Path> stream = Files.walk(root, 1)) {
            return stream.anyMatch(file -> {
                try {
                    if (!Files.isHidden(file)) {
                        return Files.isRegularFile(file) && FileTypeHelper.isExcelFile(file.getFileName().toString());
                    }
                } catch (IOException e) {
                    LOG.debug(e.getMessage(), e);
                }
                return false;
            });
        } catch (IOException e) {
            LOG.debug(e.getMessage(), e);
            return false;
        }
    }

    private static Optional<Path> getProjectDescriptor(Path root) {
        return Optional.of(RULES_XML).map(root::resolve).filter(Files::isRegularFile);
    }

    @Override
    public void close() {
        // Close repo connection after validation
        IOUtils.closeQuietly(deployRepo);
    }
}
