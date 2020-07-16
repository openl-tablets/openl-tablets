package org.openl.rules.workspace.dtr.impl;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.openl.config.ConfigNames;
import org.openl.rules.common.CommonVersion;
import org.openl.rules.project.abstraction.ADeploymentProject;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.repository.RepositoryInstatiator;
import org.openl.rules.repository.RepositoryMode;
import org.openl.rules.repository.api.BranchRepository;
import org.openl.rules.repository.api.Features;
import org.openl.rules.repository.api.FeaturesBuilder;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.FolderMapper;
import org.openl.rules.repository.api.FolderRepository;
import org.openl.rules.repository.api.Listener;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.workspace.ProjectKey;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.rules.workspace.dtr.DesignTimeRepositoryListener;
import org.openl.rules.workspace.dtr.RepositoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.PropertyResolver;

/**
 * @author Aleh Bykhavets
 */
public class DesignTimeRepositoryImpl implements DesignTimeRepository {
    private final Logger log = LoggerFactory.getLogger(DesignTimeRepositoryImpl.class);

    private static final String DESIGN_REPOSITORIES = "design-repository-configs";
    public static final String USE_REPOSITORY_FOR_DEPLOY_CONFIG = "repository.deploy-config.use-repository";
    private static final String RULES_LOCATION_CONFIG_NAME = "repository.design.base.path";
    private static final String DEPLOYMENT_CONFIGURATION_LOCATION_CONFIG_NAME = "repository.deploy-config.base.path";
    private static final String PROJECTS_FLAT_FOLDER_STRUCTURE = "repository.%s.folder-structure.flat";
    private static final String DEPLOY_CONFIG_FLAT_FOLDER_STRUCTURE = "repository.deploy-config.folder-structure.flat";

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

    private PropertyResolver propertyResolver;
    private Repository settingsRepository;

    public void setPropertyResolver(PropertyResolver propertyResolver) {
        this.propertyResolver = propertyResolver;
    }

    public void setSettingsRepository(Repository settingsRepository) {
        this.settingsRepository = settingsRepository;
    }

    public void init() {
        synchronized (projects) {
            if (repositories != null) {
                return;
            }

            repositories = new ArrayList<>();
            RepositoryListener callback = new RepositoryListener(listeners);

            rulesLocation = propertyResolver.getProperty(RULES_LOCATION_CONFIG_NAME);
            if (!rulesLocation.isEmpty() && !rulesLocation.endsWith("/")) {
                rulesLocation += "/";
            }

            String[] designRepositories = Objects.requireNonNull(propertyResolver.getProperty(DESIGN_REPOSITORIES)).split("\\s*,\\s*");
            for (String designRepositoryId : designRepositories) {
                boolean flatProjects = Boolean.parseBoolean(propertyResolver.getProperty(String.format(PROJECTS_FLAT_FOLDER_STRUCTURE, designRepositoryId)));

                Repository repository = createRepo(designRepositoryId, flatProjects, rulesLocation);

                repositories.add(repository);

                addListener(() -> {
                    synchronized (projects) {
                        projectsRefreshNeeded = true;
                    }
                });
                repository.setListener(callback);
            }

            deploymentConfigurationLocation = propertyResolver
                .getProperty(DEPLOYMENT_CONFIGURATION_LOCATION_CONFIG_NAME);
            if (!deploymentConfigurationLocation.isEmpty() && !deploymentConfigurationLocation.endsWith("/")) {
                deploymentConfigurationLocation += "/";
            }
            String repositoryForDeployConfig = propertyResolver.getProperty(USE_REPOSITORY_FOR_DEPLOY_CONFIG);
            boolean separateDeployConfigRepo = StringUtils.isBlank(repositoryForDeployConfig);
            boolean flatDeployConfig = Boolean
                .parseBoolean(propertyResolver.getProperty(DEPLOY_CONFIG_FLAT_FOLDER_STRUCTURE));
            if (!separateDeployConfigRepo) {
                Repository repository = getRepository(repositoryForDeployConfig);
                if (!(repository.supports().mappedFolders())) {
                    deployConfigRepository = repository;
                } else {
                    // Deploy config repository currently supports only flat folder structure.
                    deployConfigRepository = ((FolderMapper) repository).getDelegate();
                }
            } else {
                deployConfigRepository = createRepo(ConfigNames.DEPLOY_CONFIG,
                    flatDeployConfig, deploymentConfigurationLocation);
            }

            if (separateDeployConfigRepo) {
                deployConfigRepository.setListener(callback);
            }
        }
    }

    private Repository createRepo(String configName, boolean flatStructure, String baseFolder) {
        try {
            Repository repo = RepositoryInstatiator.newRepository(configName, propertyResolver);
            if (settingsRepository != null) {
                String setter = "setSettingsRepository";
                try {
                    Method setMethod = repo.getClass().getMethod(setter, Repository.class);
                    setMethod.invoke(repo, settingsRepository);
                } catch (NoSuchMethodException e) {
                    log.debug(e.getMessage(), e);
                }
            }

            if (!flatStructure && repo.supports().folders()) {
                // Nested folder structure is supported for FolderRepository only
                FolderRepository delegate = (FolderRepository) repo;

                RepositoryMode repositoryMode = null;
                if (configName.startsWith(ConfigNames.DEPLOY_CONFIG)) {
                    repositoryMode = RepositoryMode.DEPLOY_CONFIG;
                } else if (configName.startsWith(ConfigNames.DESIGN_CONFIG)) {
                    repositoryMode = RepositoryMode.DESIGN;
                } else if (configName.startsWith(ConfigNames.PRODUCTION)) {
                    repositoryMode = RepositoryMode.PRODUCTION;
                }
                repo = MappedRepository.create(delegate, repositoryMode, baseFolder, settingsRepository);
            }

            return repo;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
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
                new Class[] { Repository.class },
                (proxy, method, args) -> {
                    if (method.getName().startsWith("set") && method.getReturnType() == void.class) {
                        return null;
                    }
                    if ("supports".equals(method.getName()) && method.getReturnType() == Features.class) {
                        return new FeaturesBuilder(null).setVersions(false).build();
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
        LinkedList<ADeploymentProject> result = new LinkedList<>();
        Repository repository = getDeployConfigRepository();

        Collection<FileData> fileDatas;
        try {
            String path = deploymentConfigurationLocation;
            if (repository.supports().folders()) {
                fileDatas = ((FolderRepository) repository).listFolders(path);
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
    public AProject getProject(String repositoryId, String name) throws RepositoryException {
        synchronized (projects) {
            if (projectsRefreshNeeded) {
                refreshProjects();
            }

            ProjectKey projectKey = new ProjectKey(repositoryId, name.toLowerCase());

            AProject cached = projects.get(projectKey);
            if (cached != null) {
                return cached;
            } else {
                throw new RepositoryException("Project '{0}' is not found.", null, name);
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
                    FileData fileData = repository.checkHistory(projectPath, repoVersion);
                    if (fileData != null) {
                        project = new AProject(repository, fileData);
                    } else {
                        BranchRepository branchRepository = (BranchRepository) repository;
                        List<String> branches = branchRepository.getBranches(name);
                        for (String branch : branches) {
                            BranchRepository secondaryBranch = branchRepository.forBranch(branch);
                            fileData = secondaryBranch.checkHistory(projectPath, repoVersion);
                            if (fileData != null) {
                                project = new AProject(secondaryBranch, fileData);
                                break;
                            }
                        }

                        if (project == null) {
                            log.warn("Project '{}' with version '{}' is not found.", name, repoVersion);
                            project = new AProject(repository, projectPath, repoVersion);
                        }
                    }
                } catch (IOException e) {
                    log.error(e.getMessage(), e);
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
    public AProject getProject(String repositoryId, String branch, String project, String version) throws IOException {
        BranchRepository repository = ((BranchRepository) getRepository(repositoryId)).forBranch(branch);
        String projectPath = rulesLocation + project;
        return new AProject(repository, projectPath, version);
    }

    @Override
    public Collection<AProject> getProjects() {
        List<AProject> result;

        synchronized (projects) {
            refreshProjects();

            result = new ArrayList<>(projects.values());
        }

        result.sort((o1, o2) -> o1.getName().compareToIgnoreCase(o2.getName()));

        return result;
    }

    private void refreshProjects() {
        projects.clear();
        projectsVersions.clear();
        for (Repository repository : getRepositories()) {
            Collection<FileData> fileDatas;
            try {
                String path = rulesLocation;
                if (repository.supports().folders()) {
                    fileDatas = ((FolderRepository) repository).listFolders(path);
                } else {
                    fileDatas = repository.list(path);
                }
            } catch (IOException ex) {
                log.error(ex.getMessage(), ex);
                fileDatas = Collections.emptyList();
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
            return projects.containsKey(new ProjectKey(repositoryId, name.toLowerCase()));
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
                    if (repository instanceof Closeable) {
                        ((Closeable) repository).close();
                    }
                    if (deployConfigRepository == repository) {
                        deployConfigRepository = null;
                    }
                }
                repositories = null;
            }
            if (deployConfigRepository != null) {
                deployConfigRepository.setListener(null);
                if (deployConfigRepository instanceof Closeable) {
                    ((Closeable) deployConfigRepository).close();
                }
            }

            projects.clear();
            projectsVersions.clear();
        }
    }

    @Override
    public Repository getRepository(String id) {
        return getRepositories().stream().filter(repository -> id.equals(repository.getId())).findFirst().orElse(null);
    }

    @Override
    public List<Repository> getRepositories() {
        if (repositories == null) {
            // repository field can be cleared in Admin page during testing connection to a new repository
            init();
        }

        return repositories;
    }

    private Repository getDeployConfigRepository() {
        if (deployConfigRepository == null) {
            // deployConfigRepository field can be cleared in Admin page during testing connection to a new repository
            init();
        }
        return deployConfigRepository;
    }

    @Override
    public String getRulesLocation() {
        return rulesLocation;
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
