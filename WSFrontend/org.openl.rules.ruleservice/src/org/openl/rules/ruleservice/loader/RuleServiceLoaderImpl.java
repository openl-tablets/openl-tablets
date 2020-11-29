package org.openl.rules.ruleservice.loader;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystem;
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
import org.openl.rules.repository.api.FileItem;
import org.openl.rules.repository.api.FolderRepository;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.repository.exceptions.RRepositoryException;
import org.openl.rules.repository.file.FileSystemRepository;
import org.openl.rules.ruleservice.core.RuleServiceRuntimeException;
import org.openl.util.FileTypeHelper;
import org.openl.util.RuntimeExceptionWrapper;
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

    private ProjectResolver projectResolver;

    private Repository repository;
    private String deployPath = "";
    private FileSystemRepository tempRepo;
    private Path tempPath;

    /**
     * Construct a new RulesLoader for bean usage.
     */
    public RuleServiceLoaderImpl(Repository repository) throws IOException, RRepositoryException {
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
            if (projectFolder.getFileName() != null && FileTypeHelper.isZipFile(projectFolder.getFileName().toString())) {
                try {
                    FileSystem fs = FileSystems.newFileSystem(projectFolder, Thread.currentThread().getContextClassLoader());
                    projectFolder = fs.getPath("/");
                } catch (IOException e) {
                    throw RuntimeExceptionWrapper.wrap(e);
                }
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
                data = repository.check(deploymentName);
                if (data.getPath() != null) {
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
            log.debug("Loading deployement with name='{}' and version='{}'", deploymentName, versionName);
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
        List<FileData> fileDatas;
        try {
            if (repository.supports().folders()) {
                // All deployments
                fileDatas = ((FolderRepository) repository).listFolders(getDeployPath());
            } else {
                // Projects inside all deployments
                fileDatas = repository.list(getDeployPath());
            }
        } catch (IOException ex) {
            throw RuntimeExceptionWrapper.wrap(ex);
        }
        ConcurrentMap<String, IDeployment> deployments = new ConcurrentHashMap<>();
        for (FileData fileData : fileDatas) {
            String name = fileData.getName();
            String deployFolder = getDeployPath();
            String deploymentPath = name.substring(deployFolder.length());
            String[] pathEntries = deploymentPath.split("/");
            String deploymentFolderName = pathEntries[0];

            String version = fileData.getVersion();
            CommonVersionImpl commonVersion = new CommonVersionImpl(version == null ? "0" : version);

            String folderPath = getDeployPath() + deploymentFolderName;

            boolean folderStructure = isFolderStructure(folderPath);

            //FIXME: Workaround for simple deployment zip files
            Deployment deployment = null;
            if (isLocalZipFile(fileData) && isSimpleProjectDeployment(fileData)) {
                String versionName = commonVersion.getVersionName();
                final String tempDeploymentName = deploymentFolderName + "_v" + versionName;
                deployment = new Deployment(tempRepo,
                        tempDeploymentName,
                        deploymentFolderName,
                        commonVersion,
                        true);
                Path tempDeploymentPath = tempPath.resolve(tempDeploymentName).resolve(deploymentFolderName);
                try {
                    Files.createDirectories(tempDeploymentPath);
                    List<FileData> artefacts = repository.list(getDeployPath() + deploymentFolderName);
                    for (FileData artefactData : artefacts) {
                        //create sub folders
                        Path artefactPath = artefactData.getPath().getParent();
                        artefactPath = tempDeploymentPath.resolve(artefactPath.toString().substring(1));
                        Files.createDirectories(artefactPath);

                        artefactPath = artefactPath.resolve(artefactData.getPath().getFileName().toString());
                        FileItem artefactItem = repository.read(artefactData.getName());
                        try (InputStream is = artefactItem.getStream()) {
                            Files.copy(is, artefactPath);
                        }
                    }
                    deployment.refresh();
                } catch (IOException e) {
                    log.error(e.getMessage(), e);
                    deployment = null;
                }
            }

            if (deployment == null) {
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

    @Override
    public Collection<IDeployment> getDeployments2() {
        List<FileData> fileDatas;
        try {
            if (repository.supports().folders()) {
                // All deployments
                fileDatas = ((FolderRepository) repository).listFolders(getDeployPath());
            } else {
                // Projects inside all deployments
                fileDatas = repository.list(getDeployPath());
            }
        } catch (IOException ex) {
            throw RuntimeExceptionWrapper.wrap(ex);
        }

        ConcurrentMap<String, IDeployment> deployments = new ConcurrentHashMap<>();
        for (FileData fileData : fileDatas) {
            String name = fileData.getName();
            String deployFolder = getDeployPath();
            String deploymentPath = name.substring(deployFolder.length());
            String[] pathEntries = deploymentPath.split("/");
            String deploymentFolderName = pathEntries[0];

            String version = fileData.getVersion();
            CommonVersionImpl commonVersion = new CommonVersionImpl(version == null ? "0" : version);
            String folderPath = getDeployPath() + deploymentFolderName;

            IDeployment deployment;
            if (isLocalZipFile(fileData)) {
                try {
                    deployment = buildLocalDeployment(commonVersion, fileData, (FolderRepository) repository);
                } catch (IOException e) {
                    throw RuntimeExceptionWrapper.wrap(e);
                }
            } else {
                boolean folderStructure = isFolderStructure(folderPath);
                deployment = new Deployment(repository, folderPath, deploymentFolderName, commonVersion, folderStructure);
            }
            deployments.putIfAbsent(deploymentFolderName, deployment);
        }

        return deployments.values();
    }

    private boolean isLocalZipFile(FileData fileData) {
        return repository.supports().folders()
                && repository.supports().isLocal()
                && fileData.getPath() != null
                && FileTypeHelper.isZipFile(fileData.getPath().getFileName().toString());
    }

    private boolean isSimpleProjectDeployment(FileData fileData) {
        try (FileSystem zipFS = FileSystems.newFileSystem(fileData.getPath(), Thread.currentThread().getContextClassLoader())) {
            Path zipRoot = zipFS.getPath("/");
            return projectResolver.isRulesProject(zipRoot) != null;
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            return false;
        } catch (ProviderNotFoundException unused) {
            return false;
        }
    }

    private LocalDeployment buildLocalDeployment(CommonVersion commonVersion, FileData deploymentFolder, FolderRepository repository) throws IOException {
        LocalDeployment deployment;
        if (isSimpleProjectDeployment(deploymentFolder)) {
            Map<String, IProjectArtefact> resourceMap = gatherProjectResources(deploymentFolder, repository);
            LocalProject project = new LocalProject(deploymentFolder, resourceMap);
            deployment = new LocalDeployment(deploymentFolder.getName().split("/")[0], commonVersion, Collections.singletonMap(project.getName(), project));
        } else {
            List<FileData> projectFolders = repository.listFolders(deploymentFolder.getName());
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

    private Map<String, IProjectArtefact> gatherProjectResources(FileData folder, Repository repository) throws IOException {
        List<FileData> files = repository.list(folder.getName());
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
}
