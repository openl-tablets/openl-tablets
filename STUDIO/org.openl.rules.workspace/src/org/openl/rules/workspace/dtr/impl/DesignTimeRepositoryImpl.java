package org.openl.rules.workspace.dtr.impl;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Proxy;
import java.util.*;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.openl.rules.common.ArtefactPath;
import org.openl.rules.common.CommonVersion;
import org.openl.rules.common.ProjectException;
import org.openl.rules.project.abstraction.ADeploymentProject;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.AProjectArtefact;
import org.openl.rules.repository.RepositoryFactoryInstatiator;
import org.openl.rules.repository.RepositoryMode;
import org.openl.rules.repository.api.*;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.rules.workspace.dtr.DesignTimeRepositoryListener;
import org.openl.rules.workspace.dtr.RepositoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;

/**
 * @author Aleh Bykhavets
 */
public class DesignTimeRepositoryImpl implements DesignTimeRepository {
    private final Logger log = LoggerFactory.getLogger(DesignTimeRepositoryImpl.class);

    public static final String USE_SEPARATE_DEPLOY_CONFIG_REPO = "repository.deploy-config.separate-repository";
    private static final String RULES_LOCATION_CONFIG_NAME = "repository.design.base.path";
    private static final String DEPLOYMENT_CONFIGURATION_LOCATION_CONFIG_NAME = "repository.deploy-config.base.path";
    private static final String PROJECTS_FLAT_FOLDER_STRUCTURE = "repository.design.folder-structure.flat";
    private static final String PROJECTS_NESTED_FOLDER_CONFIG = "repository.design.folder-structure.configuration";
    private static final String DEPLOY_CONFIG_FLAT_FOLDER_STRUCTURE = "repository.deploy-config.folder-structure.flat";
    private static final String DEPLOY_CONFIG_NESTED_FOLDER_CONFIG = "repository.deploy-config.folder-structure.configuration";

    private volatile Repository repository;
    private volatile Repository deployConfigRepository;
    private volatile String rulesLocation;
    private volatile String deploymentConfigurationLocation;
    private volatile boolean projectsRefreshNeeded = true;

    /**
     * Project Cache
     */
    private final HashMap<String, AProject> projects = new HashMap<>();
    private final HashMap<String, AProject> projectsVersions = new HashMap<>();

    private final List<DesignTimeRepositoryListener> listeners = new ArrayList<>();

    private Environment environment;

    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    public void init() {
        synchronized (projects) {
            if (repository != null) {
                return;
            }

            rulesLocation = environment.getProperty(RULES_LOCATION_CONFIG_NAME);
            if (!rulesLocation.isEmpty() && !rulesLocation.endsWith("/")) {
                rulesLocation += "/";
            }
            deploymentConfigurationLocation = environment.getProperty(DEPLOYMENT_CONFIGURATION_LOCATION_CONFIG_NAME);
            if (!deploymentConfigurationLocation.isEmpty() && !deploymentConfigurationLocation.endsWith("/")) {
                deploymentConfigurationLocation += "/";
            }
            boolean separateDeployConfigRepo = Boolean
                .parseBoolean(environment.getProperty(USE_SEPARATE_DEPLOY_CONFIG_REPO));
            boolean flatProjects = Boolean.parseBoolean(environment.getProperty(PROJECTS_FLAT_FOLDER_STRUCTURE));
            boolean flatDeployConfig = Boolean.parseBoolean(environment.getProperty(DEPLOY_CONFIG_FLAT_FOLDER_STRUCTURE));

            repository = createRepo(RepositoryMode.DESIGN.name(), flatProjects, PROJECTS_NESTED_FOLDER_CONFIG, rulesLocation);

            if (!separateDeployConfigRepo) {
                if (flatProjects || !(repository instanceof MappedRepository)) {
                    deployConfigRepository = repository;
                } else {
                    // Deploy config repository currently supports only flat folder structure.
                    deployConfigRepository = ((MappedRepository) repository).getDelegate();
                }
            } else {
                deployConfigRepository = createRepo(RepositoryMode.DEPLOY_CONFIG.name(),
                    flatDeployConfig,
                    DEPLOY_CONFIG_NESTED_FOLDER_CONFIG,
                    deploymentConfigurationLocation);
            }

            addListener(() -> {
                synchronized (projects) {
                    projectsRefreshNeeded = true;
                }
            });

            RepositoryListener callback = new RepositoryListener(listeners);
            repository.setListener(callback);
            if (separateDeployConfigRepo) {
                deployConfigRepository.setListener(callback);
            }
        }
    }

    private Repository createRepo(String configName,
            boolean flatStructure,
            String folderConfig,
            String baseFolder) {
        try {
            Repository repo = RepositoryFactoryInstatiator.newFactory(environment, configName);
            if (!flatStructure && repo.supports().folders()) {
                // Nested folder structure is supported for FolderRepository only
                FolderRepository delegate = (FolderRepository) repo;
                String configFile = environment.getProperty(folderConfig);

                MappedRepository mappedRepository = new MappedRepository();
                mappedRepository.setDelegate(delegate);
                mappedRepository.setRepositoryMode(null);
                mappedRepository.setConfigFile(configFile);
                mappedRepository.setBaseFolder(baseFolder);
                mappedRepository.initialize();
                repo = mappedRepository;
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
    public AProjectArtefact getArtefactByPath(ArtefactPath artefactPath) throws ProjectException {
        String projectName = artefactPath.segment(0);
        AProject ralProject = getProject(projectName);

        ArtefactPath pathInProject = artefactPath.withoutFirstSegment();
        return ralProject.getArtefactByPath(pathInProject);
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
    public AProject getProject(String name) throws RepositoryException {
        AProject project;
        synchronized (projects) {
            if (projectsRefreshNeeded) {
                refreshProjects();
            }

            if (!hasProject(name)) {
                throw new RepositoryException("Project '{0}' is not found.", null, name);
            }

            AProject cached = projects.get(name.toLowerCase());
            if (cached != null) {
                return cached;
            }

            // TODO: Seems we never reach here. Is the code below really needed?
            project = new AProject(getRepository(), rulesLocation + name);
            projects.put(project.getName().toLowerCase(), project);
        }
        return project;
    }

    @Override
    public AProject getProject(String name, CommonVersion version) {
        String repoVersion = version.getVersionName();
        String key = String.format("%s:%s", name, repoVersion);
        AProject project = projectsVersions.get(key);

        if (project == null) {
            Repository repository = getRepository();
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
        Collection<FileData> fileDatas;
        Repository repository = getRepository();
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
        projects.clear();
        projectsVersions.clear();
        for (FileData fileData : fileDatas) {
            AProject project = new AProject(repository, fileData);
            // get from the repository
            projects.put(project.getName().toLowerCase(), project);
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
    public boolean hasProject(String name) {
        synchronized (projects) {
            if (projectsRefreshNeeded) {
                refreshProjects();
            }
            return projects.containsKey(name.toLowerCase());
        }
    }

    // --- private

    @Override
    public void addListener(DesignTimeRepositoryListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeListener(DesignTimeRepositoryListener listener) {
        listeners.remove(listener);
    }

    @Override
    public List<DesignTimeRepositoryListener> getListeners() {
        return listeners;
    }

    /**
     * destroy-method
     */
    public void destroy() throws Exception {
        synchronized (projects) {
            if (repository != null) {
                repository.setListener(null);
                if (repository instanceof Closeable) {
                    ((Closeable) repository).close();
                }
                if (deployConfigRepository == repository) {
                    deployConfigRepository = null;
                }
                repository = null;
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
    public Repository getRepository() {
        if (repository == null) {
            // repository field can be cleared in Admin page during testing connection to a new repository
            init();
        }
        return repository;
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
            for (DesignTimeRepositoryListener listener : listeners) {
                listener.onRepositoryModified();
            }
        }
    }
}
