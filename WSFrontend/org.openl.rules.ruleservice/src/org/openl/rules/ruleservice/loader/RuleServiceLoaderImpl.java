package org.openl.rules.ruleservice.loader;

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.ProviderNotFoundException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.annotation.PreDestroy;

import org.openl.rules.common.CommonVersion;
import org.openl.rules.common.ProjectException;
import org.openl.rules.common.impl.CommonVersionImpl;
import org.openl.rules.deploy.LocalDeployment;
import org.openl.rules.deploy.LocalProject;
import org.openl.rules.deploy.LocalProjectResource;
import org.openl.rules.project.abstraction.Deployment;
import org.openl.rules.project.abstraction.IDeployment;
import org.openl.rules.project.abstraction.IProject;
import org.openl.rules.project.abstraction.IProjectArtefact;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.project.resolving.ProjectResolver;
import org.openl.rules.project.resolving.ProjectResolvingException;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.FolderRepository;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.repository.file.FileSystemRepository;
import org.openl.rules.repository.zip.ZippedLocalRepository;
import org.openl.rules.ruleservice.core.RuleServiceRuntimeException;
import org.openl.util.FileTypeHelper;
import org.openl.util.RuntimeExceptionWrapper;
import org.openl.util.StringUtils;
import org.openl.util.ZipUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.FileSystemUtils;

/**
 * Wrapper on data source that gives access to data source and resolves the OpenL projects/modules inside the projects.
 * Contains own storage for all projects that is used in services.
 *
 * @author Marat Kamalov
 */
public class RuleServiceLoaderImpl implements RuleServiceLoader {
    private final Logger log = LoggerFactory.getLogger(RuleServiceLoaderImpl.class);

    private final ProjectResolver projectResolver;
    private final Repository repository;
    private final FileSystemRepository tempRepo;
    private final Path tempPath;
    private String deployPath = "";

    /**
     * Construct a new RulesLoader for bean usage.
     */
    public RuleServiceLoaderImpl(Repository repository) throws IOException {
        tempPath = Files.createTempDirectory("rules-deploy_");
        log.info("Local temporary folder location is: {}", tempPath);
        tempRepo = new FileSystemRepository();
        tempRepo.setUri(tempPath.toString());
        tempRepo.initialize();
        this.projectResolver = ProjectResolver.getInstance();
        this.repository = repository;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<Module> resolveModulesForProject(String deploymentName,
            CommonVersion deploymentVersion,
            String projectName) {
        Objects.requireNonNull(deploymentName, "deploymentName cannot be null");
        Objects.requireNonNull(deploymentVersion, "deploymentVersion cannot be null");
        Objects.requireNonNull(projectName, "projectName cannot be null");

        log.debug("Resolving modules for deployment (name='{}', version='{}', projectName='{}')",
            deploymentName,
            deploymentVersion.getVersionName(),
            projectName);

        IDeployment localDeployment = getDeployment(deploymentName, deploymentVersion);
        IProject project = localDeployment.getProject(projectName);
        if (project == null) {
            throw new RuleServiceRuntimeException(
                String.format("Project '%s' is not found in deployment '%s'.", projectName, deploymentName));
        }
        Path projectFolder;
        if (project instanceof LocalProject) {
            projectFolder = ((LocalProject) project).getData().getPath();
            if (projectFolder.getFileName() != null && (FileTypeHelper.isZipFile(
                projectFolder.getFileName().toString()) || ZippedLocalRepository.zipArchiveFilter(projectFolder))) {

                FileSystem fs = FileSystems.getFileSystem(ZipUtils.toJarURI(projectFolder));
                projectFolder = fs.getPath("/");
            }
        } else {
            String stringValue = project.getArtefactPath().getStringValue();
            projectFolder = tempPath.resolve(stringValue);
        }
        List<Module> result = Collections.emptyList();
        try {
            ProjectDescriptor projectDescriptor = projectResolver.resolve(projectFolder);
            if (projectDescriptor != null) {
                List<Module> modules = projectDescriptor.getModules();
                result = Collections.unmodifiableList(modules);
            }
        } catch (ProjectResolvingException e) {
            log.error("Project resolving has been failed.", e);
        }
        return result;
    }

    @Override
    public IDeployment getDeployment(String deploymentName, CommonVersion version) {
        if (repository.supports().isLocal() && repository.supports().folders()) {
            FileData data;
            try {
                data = repository.check(getDeployPath() + deploymentName);
                if (data != null && data.getPath() != null) {
                    return buildLocalDeployment(version, data, (FolderRepository) repository);
                }
            } catch (IOException e) {
                throw RuntimeExceptionWrapper.wrap(e);
            }
        }
        String versionName = version.getVersionName();
        Deployment loadedDeployment = new Deployment(tempRepo,
            deploymentName + "_v" + versionName,
            deploymentName,
            version,
            true);

        if (loadedDeployment.getProjects().isEmpty()) {
            log.debug("Loading deployment with name='{}' and version='{}'", deploymentName, versionName);
            String folderPath = getDeployPath() + deploymentName;
            boolean folderStructure = isFolderStructure(folderPath);
            Deployment deployment = new Deployment(repository, folderPath, deploymentName, version, folderStructure);

            try {
                loadedDeployment.update(deployment, null);
                loadedDeployment.refresh();
            } catch (ProjectException e) {
                log.warn("Exception occurs on loading deployment with name='{}' and version='{}' from data source.",
                    deploymentName,
                    versionName,
                    e);
                throw new RuleServiceRuntimeException(e);
            }
        }

        return loadedDeployment;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<IDeployment> getDeployments() {
        List<FileData> fileData;
        try {
            if (repository.supports().folders()) {
                // All deployments
                fileData = ((FolderRepository) repository).listFolders(getDeployPath());
            } else {
                // Projects inside all deployments
                fileData = repository.list(getDeployPath());
            }
        } catch (IOException ex) {
            throw RuntimeExceptionWrapper.wrap(ex);
        }

        ConcurrentMap<String, IDeployment> deployments = new ConcurrentHashMap<>();
        for (FileData fd : fileData) {
            String name = fd.getName();
            String deployFolder = getDeployPath();
            String deploymentPath = name.substring(deployFolder.length());
            String[] pathEntries = deploymentPath.split("/");
            String deploymentFolderName = pathEntries[0];

            String version = fd.getVersion();
            CommonVersionImpl commonVersion = new CommonVersionImpl(version == null ? "0" : version);
            String folderPath = getDeployPath() + deploymentFolderName;

            IDeployment deployment;
            if (isLocalZipFile(fd)) {
                try {
                    deployment = buildLocalDeployment(commonVersion, fd, (FolderRepository) repository);
                } catch (IOException e) {
                    throw RuntimeExceptionWrapper.wrap(e);
                }
            } else {
                boolean folderStructure = isFolderStructure(folderPath);
                deployment = new Deployment(repository,
                    folderPath,
                    deploymentFolderName,
                    commonVersion,
                    folderStructure);
            }
            deployments.putIfAbsent(deploymentFolderName, deployment);
        }

        return deployments.values();
    }

    private boolean isLocalZipFile(FileData fileData) {
        return repository.supports().folders() && repository.supports()
            .isLocal() && fileData.getPath() != null && (FileTypeHelper
                .isZipFile(fileData.getPath().getFileName().toString()) || ZippedLocalRepository
                    .zipArchiveFilter(fileData.getPath()));
    }

    private boolean isSimpleProjectDeployment(FileData fileData) {
        URI jarURI = ZipUtils.toJarURI(fileData.getPath());
        try {
            Path zipRoot = FileSystems.getFileSystem(jarURI).getPath("/");
            return projectResolver.isRulesProject(zipRoot) != null;
        } catch (FileSystemNotFoundException ignored) {
            try (FileSystem fs = FileSystems.newFileSystem(jarURI, Collections.emptyMap());) {
                return projectResolver.isRulesProject(fs.getPath("/")) != null;
            } catch (IOException | UnsupportedOperationException | ProviderNotFoundException e) {
                return false;
            }
        }
    }

    private LocalDeployment buildLocalDeployment(CommonVersion commonVersion,
            FileData deploymentFolder,
            FolderRepository repository) throws IOException {
        LocalDeployment deployment;
        if (isSimpleProjectDeployment(deploymentFolder)) {
            Map<String, IProjectArtefact> resourceMap = gatherProjectResources(deploymentFolder, repository);
            LocalProject project = new LocalProject(deploymentFolder, resourceMap);
            deployment = new LocalDeployment(deploymentFolder.getName().split("/")[0],
                commonVersion,
                Collections.singletonMap(project.getName(), project));
        } else {
            List<FileData> projectFolders = repository.listFolders(getDeployPath() + deploymentFolder.getName());
            Map<String, IProject> projectMap = new HashMap<>();
            for (FileData projectFolder : projectFolders) {
                Map<String, IProjectArtefact> resourceMap = gatherProjectResources(projectFolder, repository);
                LocalProject project = new LocalProject(projectFolder, resourceMap);
                projectMap.put(project.getName(), project);
            }
            deployment = new LocalDeployment(deploymentFolder.getName().split("/")[0], commonVersion, projectMap);
        }
        return deployment;
    }

    private Map<String, IProjectArtefact> gatherProjectResources(FileData folder,
            Repository repository) throws IOException {
        List<FileData> files = repository.list(getDeployPath() + folder.getName());
        Map<String, IProjectArtefact> resourceMap = new HashMap<>();
        for (FileData file : files) {
            String resourceName = file.getName().substring(folder.getName().length() + 1);
            LocalProjectResource resource = new LocalProjectResource(resourceName, repository.read(file.getName()));
            resourceMap.put(resource.getName(), resource);
        }
        return resourceMap;
    }

    @Override
    public void setListener(DataSourceListener dataSourceListener) {
        if (dataSourceListener == null) {
            repository.setListener(null);
        } else {
            repository.setListener(dataSourceListener::onDeploymentAdded);
        }
    }

    /**
     * For Spring framework
     */
    @PreDestroy
    public void destroy() throws Exception {
        log.debug("Data source releasing");
        if (repository instanceof Closeable) {
            ((Closeable) repository).close();
        }
        tempRepo.close();
        try {
            FileSystemUtils.deleteRecursively(tempPath);
        } catch (Exception e) {
            log.error("Cannot delete temporary directory", e);
        }
    }

    public void setDeployPath(String deployPath) {
        this.deployPath = deployPath.isEmpty() || deployPath.endsWith("/") ? deployPath : deployPath + "/";
    }

    private boolean isFolderStructure(String deploymentFolderPath) {
        boolean folderStructure;
        try {
            if (repository.supports().folders()) {
                folderStructure = !((FolderRepository) repository).listFolders(deploymentFolderPath + "/").isEmpty();
            } else {
                folderStructure = false;
            }
        } catch (IOException e) {
            throw RuntimeExceptionWrapper.wrap(e);
        }
        return folderStructure;
    }

    private String getDeployPath() {
        if (repository.supports().isLocal()) {
            // NOTE deployment path isn't required for LocalRepository. It must be specified within URI
            return "";
        }
        return deployPath;
    }

    @Override
    public String getLogicalProjectFolder(String realFolderPath) {
        if (StringUtils.isBlank(realFolderPath) || repository.supports().isLocal()) {
            return realFolderPath;
        }
        final String baseDeployFolder = getDeployPath();
        if (!realFolderPath.startsWith(baseDeployFolder)) {
            return realFolderPath;
        }
        return realFolderPath.substring(baseDeployFolder.length());
    }
}
