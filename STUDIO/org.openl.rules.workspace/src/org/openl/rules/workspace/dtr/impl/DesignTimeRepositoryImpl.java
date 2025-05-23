package org.openl.rules.workspace.dtr.impl;

import java.io.IOException;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.PropertyResolver;

import org.openl.rules.common.CommonVersion;
import org.openl.rules.common.ProjectException;
import org.openl.rules.project.abstraction.ADeploymentProject;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.AProjectFolder;
import org.openl.rules.project.abstraction.Comments;
import org.openl.rules.repository.RepositoryInstatiator;
import org.openl.rules.repository.RepositoryMode;
import org.openl.rules.repository.api.BranchRepository;
import org.openl.rules.repository.api.Features;
import org.openl.rules.repository.api.FeaturesBuilder;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.Listener;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.repository.api.RepositorySettings;
import org.openl.rules.repository.api.RepositorySettingsAware;
import org.openl.rules.workspace.ProjectKey;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.rules.workspace.dtr.DesignTimeRepositoryListener;
import org.openl.rules.workspace.dtr.FolderMapper;
import org.openl.rules.workspace.dtr.RepositoryException;
import org.openl.util.IOUtils;
import org.openl.util.StringUtils;

/**
 * @author Aleh Bykhavets
 */
public class DesignTimeRepositoryImpl implements DesignTimeRepository {
    private static final Logger LOG = LoggerFactory.getLogger(DesignTimeRepositoryImpl.class);

    private static final String DESIGN_REPOSITORIES = "design-repository-configs";

    private static final String PRODUCTION_REPOSITORIES = "production-repository-configs";
    public static final String USE_REPOSITORY_FOR_DEPLOY_CONFIG = "repository.deploy-config.use-repository";

    private volatile List<Repository> repositories;
    private volatile Repository deployConfigRepository;
    private volatile String rulesLocation;
    private volatile String deploymentConfigurationLocation;
    private volatile boolean projectsRefreshNeeded = true;

    /**
     * Project Cache
     */
    private final HashMap<ProjectKey, AProject> projects = new HashMap<>();
    private final HashMap<ProjectKey, AProject> projectsVersions = new HashMap<>();

    private final List<DesignTimeRepositoryListener> listeners = new ArrayList<>();

    private final PropertyResolver propertyResolver;
    private final RepositorySettings repositorySettings;

    private final List<String> exceptions = new ArrayList<>();

    public DesignTimeRepositoryImpl(PropertyResolver propertyResolver, RepositorySettings repositorySettings) {
        this.propertyResolver = propertyResolver;
        this.repositorySettings = repositorySettings;
    }

    public void init() throws RepositoryException {
        synchronized (projects) {
            if (repositories != null) {
                return;
            }

            repositories = new ArrayList<>();
            RepositoryListener callback = new RepositoryListener(listeners);

            rulesLocation = getBasePath("design");
            String[] designRepositories = Objects.requireNonNull(propertyResolver.getProperty(DESIGN_REPOSITORIES))
                    .split("\\s*,\\s*");
            for (String repoId : designRepositories) {

                Repository repository = createRepo(repoId, rulesLocation);

                repositories.add(repository);

                addListener(() -> {
                    synchronized (projects) {
                        projectsRefreshNeeded = true;
                    }
                });
                repository.setListener(callback);
            }
            repositories = repositories.stream()
                    .filter(r -> Objects.nonNull(r.getName()))
                    .sorted(Comparator.comparing(Repository::getName, String.CASE_INSENSITIVE_ORDER))
                    .collect(Collectors.toList());

            if (StringUtils.isNotBlank(propertyResolver.getProperty(PRODUCTION_REPOSITORIES))) {
                deploymentConfigurationLocation = getBasePath(RepositoryMode.DEPLOY_CONFIG.getId());
                String repositoryForDeployConfig = propertyResolver.getProperty(USE_REPOSITORY_FOR_DEPLOY_CONFIG);
                boolean separateDeployConfigRepo = StringUtils.isBlank(repositoryForDeployConfig);
                if (!separateDeployConfigRepo) {
                    Repository repository = getRepository(repositoryForDeployConfig);
                    if (repository != null) {
                        if (!(repository.supports().mappedFolders())) {
                            deployConfigRepository = repository;
                        } else {
                            // Deploy config repository currently supports only flat folder structure.
                            deployConfigRepository = ((FolderMapper) repository).getDelegate();
                        }
                    } else {
                        throw new RepositoryException(
                                String.format("Cannot read the deploy repository '%s'.", repositoryForDeployConfig));
                    }
                } else {
                    deployConfigRepository = createRepo(RepositoryMode.DEPLOY_CONFIG.getId(),
                            deploymentConfigurationLocation);
                }

                if (separateDeployConfigRepo) {
                    deployConfigRepository.setListener(callback);
                }
            }
        }
    }

    private String getBasePath(String configName) {
        String repoPrefix = Comments.REPOSITORY_PREFIX + configName;
        var basePath = propertyResolver.getProperty(repoPrefix + ".base.path");
        if (StringUtils.isNotEmpty(basePath) && !basePath.endsWith("/")) {
            basePath += "/";
        }
        return basePath;
    }

    protected Repository createRepo(String configName, String baseFolder) {
        Repository repo = null;
        try {
            String repoPrefix = Comments.REPOSITORY_PREFIX + configName;
            repo = RepositoryInstatiator.newRepository(repoPrefix, propertyResolver::getProperty);
            if (repo instanceof RepositorySettingsAware) {
                ((RepositorySettingsAware) repo).setRepositorySettings(repositorySettings);
            }

            String flatValue = propertyResolver.getProperty(repoPrefix + ".folder-structure.flat");
            boolean flatStructure = StringUtils.isBlank(flatValue) || Boolean.parseBoolean(flatValue);

            if (!flatStructure && repo.supports().folders()) {
                // Nested folder structure is supported for FolderRepository only
                repo = MappedRepository.create(repo, baseFolder, repositorySettings);
            }

            return repo;
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            // If exception is thrown, we must close repository in this method.
            // If no exception, repository will be closed later.
            if (repo != null) {
                IOUtils.closeQuietly(repo);
            }

            Throwable rootCause = ExceptionUtils.getRootCause(e);
            if (rootCause == null) {
                rootCause = e;
            }
            String message;
            if (rootCause.getMessage() == null || !(rootCause instanceof IOException)) {
                // For some exceptions like ClassNotFoundException the messages aren't understandable for a user. Use
                // default.
                message = "Repository configuration is incorrect. Please change configuration.";
            } else {
                message = rootCause.getMessage();
            }

            return (Repository) Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class[]{Repository.class},
                    (proxy, method, args) -> {
                        final String methodName = method.getName();
                        final Class<?> returnType = method.getReturnType();
                        if (methodName.startsWith("set") && returnType == void.class) {
                            return null;
                        } else if ("supports".equals(methodName) && returnType == Features.class) {
                            return new FeaturesBuilder(null).setVersions(false).build();
                        } else if ("close".equals(methodName) && returnType == void.class && args == null) {
                            return null;
                        } else if ("getId".equals(methodName) && returnType == String.class) {
                            return configName;
                        }
                        String repoName = propertyResolver.getProperty(Comments.REPOSITORY_PREFIX + configName + ".name");
                        if ("getName".equals(methodName) && returnType == String.class) {
                            return repoName;
                        }
                        throw new IllegalStateException(message);
                    });
        }
    }

    @Override
    public ADeploymentProject.Builder createDeploymentConfigurationBuilder(String name) {
        return new ADeploymentProject.Builder(getDeployConfigRepository(), deploymentConfigurationLocation + name);
    }

    @Override
    public List<ADeploymentProject> getDDProjects() throws RepositoryException {
        if (!hasDeployConfigRepo()) {
            return Collections.emptyList();
        }
        LinkedList<ADeploymentProject> result = new LinkedList<>();
        Repository repository = getDeployConfigRepository();

        Collection<FileData> fileDatas;
        try {
            String path = deploymentConfigurationLocation;
            if (repository.supports().folders()) {
                fileDatas = repository.listFolders(path);
            } else {
                fileDatas = repository.list(path);
            }
        } catch (IOException e) {
            throw new RepositoryException("Cannot read the deploy repository.", e);
        }
        for (FileData fileData : fileDatas) {
            result.add(new ADeploymentProject(repository, fileData));
        }
        return result;
    }

    @Override
    public ADeploymentProject getDDProject(String name) throws RepositoryException {
        if (!hasDeployConfigRepo()) {
            return null;
        }
        Repository repository = getDeployConfigRepository();
        try {
            return Optional.ofNullable(repository.check(deploymentConfigurationLocation + name))
                    .map(fileData -> new ADeploymentProject(repository, fileData))
                    .orElse(null);
        } catch (IOException ex) {
            throw new RepositoryException("Cannot read the deploy configuration.", ex);
        }
    }

    @Override
    public AProject getProject(String repositoryId, String name) throws ProjectException {
        synchronized (projects) {
            if (projectsRefreshNeeded) {
                refreshProjects();
            }

            ProjectKey projectKey = new ProjectKey(repositoryId, name.toLowerCase());

            AProject cached = projects.get(projectKey);
            if (cached != null) {
                return cached;
            } else {
                Optional<AProject> project = projects.values()
                        .stream()
                        .filter(p -> p.getRepository().getId().equals(repositoryId) && p.getBusinessName().equals(name))
                        .findFirst();
                if (project.isPresent()) {
                    return project.get();
                }
                throw new ProjectException("Project '{0}' is not found.", null, name);
            }
        }
    }

    @Override
    public AProject getProject(String repositoryId, String name, CommonVersion version) {
        String repoVersion = version.getVersionName();
        ProjectKey key = new ProjectKey(repositoryId, String.format("%s:%s", name, repoVersion));
        AProject project = projectsVersions.get(key);

        if (project == null) {
            Repository repository = getRepository(repositoryId);
            String projectPath = rulesLocation + name;

            if (repository.supports().branches()) {
                try {
                    if (repository.supports().mappedFolders()) {
                        Optional<AProject> projectOptional = projects.values()
                                .stream()
                                .filter(
                                        p -> p.getRepository().getId().equals(repositoryId) && p.getBusinessName().equals(name))
                                .findFirst();
                        if (projectOptional.isPresent()) {
                            String realPath = projectOptional.get().getRealPath();
                            projectPath = ((FolderMapper) repository).findMappedName(realPath);
                        }
                    }
                    FileData fileData = repository.checkHistory(projectPath, repoVersion);
                    if (fileData != null) {
                        project = new AProject(repository, fileData);
                    } else {
                        BranchRepository branchRepository = (BranchRepository) repository;
                        List<String> branches = branchRepository.getBranches(projectPath);
                        for (String branch : branches) {
                            BranchRepository secondaryBranch = branchRepository.forBranch(branch);
                            fileData = secondaryBranch.checkHistory(projectPath, repoVersion);
                            if (fileData != null) {
                                project = new AProject(secondaryBranch, fileData);
                                break;
                            }
                        }

                        if (project == null) {
                            LOG.warn("Project '{}' with version '{}' is not found.", name, repoVersion);
                            project = new AProject(repository, projectPath, repoVersion);
                        }
                    }
                } catch (IOException e) {
                    LOG.error(e.getMessage(), e);
                    project = new AProject(repository, projectPath, repoVersion);
                }
            } else {
                project = new AProject(repository, projectPath, repoVersion);
            }

            projectsVersions.put(key, project);
        }
        return project;
    }

    @Override
    public AProject getProjectByPath(String repositoryId,
                                     String branch,
                                     String path,
                                     String version) throws IOException {
        Collection<AProject> allProjects;
        synchronized (projects) {
            if (projectsRefreshNeeded) {
                refreshProjects();
            }

            allProjects = new ArrayList<>(projects.values());
        }

        Optional<AProject> project = allProjects.stream()
                .filter(p -> p.getRepository().getId().equals(repositoryId) && p.getRealPath().equals(path))
                .findFirst();
        if (project.isPresent()) {
            Repository repository = project.get().getRepository();
            if (branch != null && repository.supports().branches()) {
                repository = ((BranchRepository) repository).forBranch(branch);
            }
            return new AProject(repository, project.get().getFolderPath(), version);
        } else {
            return null;
        }
    }

    @Override
    public void refresh() {
        synchronized (projects) {
            projectsRefreshNeeded = true;
        }
    }

    @Override
    public Collection<AProject> getProjects() {
        List<AProject> result;

        synchronized (projects) {
            if (projectsRefreshNeeded) {
                refreshProjects();
            }

            result = new ArrayList<>(projects.values());
        }

        result.sort(Comparator.comparing(AProjectFolder::getName, String.CASE_INSENSITIVE_ORDER));

        return result;
    }

    @Override
    public List<? extends AProject> getProjects(String repositoryId) {
        synchronized (projects) {
            if (projectsRefreshNeeded) {
                refreshProjects();
            }
            return projects.entrySet()
                    .stream()
                    .filter(entry -> Objects.equals(repositoryId, entry.getKey().getRepositoryId()))
                    .map(Map.Entry::getValue)
                    .sorted(Comparator.comparing(AProjectFolder::getName, String.CASE_INSENSITIVE_ORDER))
                    .collect(Collectors.toList());
        }
    }

    private void refreshProjects() {
        projects.clear();
        projectsVersions.clear();
        exceptions.clear();
        for (Repository repository : repositories) {
            Collection<FileData> fileDatas = Collections.emptyList();
            try {
                String path = rulesLocation;
                if (repository.supports().folders()) {
                    fileDatas = repository.listFolders(path);
                } else {
                    fileDatas = repository.list(path);
                }
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
                exceptions.add(String.format("Repository '%s' : %s", repository.getName(), e.getMessage()));
            }
            for (FileData fileData : fileDatas) {
                AProject project = new AProject(repository, fileData);
                // FIXME: use project path, not name
                projects.put(new ProjectKey(repository.getId(), project.getName().toLowerCase()), project);
            }
        }

        projectsRefreshNeeded = false;
    }

    @Override
    public boolean hasDDProject(String name) {
        try {
            if (!hasDeployConfigRepo()) {
                return false;
            }
            return getDeployConfigRepository().check(deploymentConfigurationLocation + name) != null;
        } catch (IOException ex) {
            return false;
        }
    }

    @Override
    public boolean hasProject(String repositoryId, String name) {
        synchronized (projects) {
            if (projectsRefreshNeeded) {
                refreshProjects();
            }
            // Check full name for mapped repositories
            if (projects.containsKey(new ProjectKey(repositoryId, name.toLowerCase()))) {
                return true;
            }

            // Check business name
            return projects.values()
                    .stream()
                    .anyMatch(p -> p.getRepository().getId().equals(repositoryId) && p.getBusinessName().equals(name));
        }
    }

    // --- private

    @Override
    public void addListener(DesignTimeRepositoryListener listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }

    @Override
    public void removeListener(DesignTimeRepositoryListener listener) {
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }

    /**
     * destroy-method
     */
    public void destroy() throws Exception {
        synchronized (projects) {
            if (repositories != null) {
                for (Repository repository : repositories) {
                    repository.setListener(null);
                    repository.close();
                    if (deployConfigRepository == repository) {
                        deployConfigRepository = null;
                    }
                }
                repositories = null;
            }
            if (deployConfigRepository != null) {
                deployConfigRepository.setListener(null);
                deployConfigRepository.close();
            }

            projects.clear();
            projectsVersions.clear();
        }
    }

    @Override
    public Repository getRepository(String id) {
        return repositories.stream()
                .filter(repository -> Objects.equals(id, repository.getId()))
                .findFirst()
                .orElse(null);
    }

    @Override
    public List<Repository> getRepositories() {
        return repositories;
    }

    @Override
    public boolean hasDeployConfigRepo() {
        return getDeployConfigRepository() != null;
    }

    @Override
    public Repository getDeployConfigRepository() {
        return deployConfigRepository;
    }

    @Override
    public String getRulesLocation() {
        return rulesLocation;
    }

    @Override
    public String getDeployConfigLocation() {
        return deploymentConfigurationLocation;
    }

    @Override
    public List<String> getExceptions() {
        return exceptions;
    }

    private static class RepositoryListener implements Listener {
        private final List<DesignTimeRepositoryListener> listeners;

        private RepositoryListener(List<DesignTimeRepositoryListener> listeners) {
            this.listeners = listeners;
        }

        @Override
        public void onChange() {
            List<DesignTimeRepositoryListener> localListeners;
            synchronized (listeners) {
                localListeners = new ArrayList<>(listeners);
            }
            for (DesignTimeRepositoryListener listener : localListeners) {
                listener.onRepositoryModified();
            }
        }
    }
}
