package org.openl.rules.ruleservice.loader;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.Deployment;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.project.resolving.ProjectResolver;
import org.openl.rules.project.resolving.ProjectResolvingException;
import org.openl.rules.repository.LocalRepositoryFactory;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.FolderRepository;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.repository.exceptions.RRepositoryException;
import org.openl.rules.repository.file.FileSystemRepository;
import org.openl.rules.ruleservice.core.RuleServiceRuntimeException;
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

    private Map<String, Deployment> cacheForGetDeployment = new HashMap<>();
    private Repository repository;
    private String deployPath;
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
        this.projectResolver = ProjectResolver.instance();
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

        Deployment localDeployment = getDeployment(deploymentName, deploymentVersion);
        AProject project = localDeployment.getProject(projectName);
        if (project == null) {
            throw new RuleServiceRuntimeException(
                String.format("Project '%s' is not found in deployment '%s'.", projectName, deploymentName));
        }
        String stringValue = project.getArtefactPath().getStringValue();
        File projectFolder = tempPath.resolve(stringValue).toFile();
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
    public Deployment getDeployment(String deploymentName, CommonVersion deploymentVersion) {
        Deployment localDeployment = getDeploymentFromCache(deploymentName, deploymentVersion);
        if (localDeployment == null) {
            String folderPath = getDeployPath() + deploymentName;
            boolean folderStructure = isFolderStructure(folderPath);
            Deployment deployment = new Deployment(repository,
                folderPath,
                deploymentName,
                deploymentVersion,
                folderStructure);
            localDeployment = loadDeployment(deployment);
        }
        return localDeployment;
    }

    /**
     * Generates folder name for deployment by given deployment name and common version.
     *
     * @return folder name
     */
    private String getDeploymentFolderName(String deploymentName, CommonVersion version) {
        return deploymentName + "_v" + version.getVersionName();
    }

    /**
     * Gets deployment from storage. If deployment does not exists in storage returns null.
     *
     * @return deployment from storage or null if doens't exists
     */
    Deployment getDeploymentFromCache(String deploymentName, CommonVersion version) {
        log.debug("Getting deployment with name='{}' and version='{}'", deploymentName, version.getVersionName());
        String deploymentFolderName = getDeploymentFolderName(deploymentName, version);
        return cacheForGetDeployment.get(deploymentFolderName);
    }

    /**
     * Loads deployment to local file system from tempRepo.
     *
     * @return loaded deployment
     */
    Deployment loadDeployment(Deployment deployment) {
        Objects.requireNonNull(deployment, "deployment cannot be null");

        String deploymentName = deployment.getDeploymentName();
        CommonVersion version = deployment.getCommonVersion();
        String versionName = deployment.getVersion().getVersionName();
        log.debug("Loading deployement with name='{}' and version='{}'", deploymentName, versionName);

        String deploymentFolderName = getDeploymentFolderName(deploymentName, version);
        Deployment loadedDeployment = new Deployment(tempRepo, deploymentFolderName, deploymentName, version, true);
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

        cacheForGetDeployment.put(deploymentFolderName, loadedDeployment);

        log.debug("Deployment with name='{}' and version='{}' has been made on local storage and put to cache.",
            deploymentName,
            versionName);
        return loadedDeployment;
    }

    /**
     * Check to existing deployment in local temporary folder.
     *
     * @return true if and only if the deployment exists; false otherwise
     */
    boolean containsDeployment(String deploymentName, CommonVersion version) {
        return cacheForGetDeployment.containsKey(getDeploymentFolderName(deploymentName, version));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<Deployment> getDeployments() {
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
        ConcurrentMap<String, Deployment> deployments = new ConcurrentHashMap<>();
        for (FileData fileData : fileDatas) {
            String deploymentFolderName = fileData.getName().substring(getDeployPath().length()).split("/")[0];

            String version = fileData.getVersion();
            CommonVersionImpl commonVersion = new CommonVersionImpl(version == null ? "0.0.0" : version);

            String folderPath = getDeployPath() + deploymentFolderName;

            boolean folderStructure = isFolderStructure(folderPath);

            Deployment deployment = new Deployment(repository,
                folderPath,
                deploymentFolderName,
                commonVersion,
                folderStructure);
            deployments.putIfAbsent(deploymentFolderName, deployment);
        }

        return deployments.values();
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
        FileSystemUtils.deleteRecursively(tempPath);
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
        if (repository instanceof LocalRepositoryFactory) {
            // NOTE deployment path isn't required for LocalRepository. It must be specified within URI
            return "";
        }
        return deployPath;
    }
}
