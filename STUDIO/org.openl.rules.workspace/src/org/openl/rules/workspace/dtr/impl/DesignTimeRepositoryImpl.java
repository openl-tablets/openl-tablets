package org.openl.rules.workspace.dtr.impl;

import java.io.Closeable;
import java.io.IOException;
import java.util.*;

import org.openl.rules.common.ArtefactPath;
import org.openl.rules.common.CommonVersion;
import org.openl.rules.common.ProjectException;
import org.openl.rules.project.abstraction.ADeploymentProject;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.AProjectArtefact;
import org.openl.rules.repository.RepositoryFactoryInstatiator;
import org.openl.rules.repository.RepositoryMode;
import org.openl.rules.repository.api.*;
import org.openl.rules.repository.exceptions.RRepositoryException;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.rules.workspace.dtr.DesignTimeRepositoryListener;
import org.openl.rules.workspace.dtr.RepositoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Aleh Bykhavets
 */
public class DesignTimeRepositoryImpl implements DesignTimeRepository {
    private final Logger log = LoggerFactory.getLogger(DesignTimeRepositoryImpl.class);

    public static final String USE_SEPARATE_DEPLOY_CONFIG_REPO = "deploy-config-repository.separate-repository";
    private static final String RULES_LOCATION_CONFIG_NAME = "design-repository.base.path";
    private static final String DEPLOYMENT_CONFIGURATION_LOCATION_CONFIG_NAME = "deploy-config-repository.base.path";
    private static final String PROJECTS_FLAT_FOLDER_STRUCTURE = "design-repository.folder-structure.flat";
    private static final String PROJECTS_NESTED_FOLDER_CONFIG = "design-repository.folder-structure.configuration";
    private static final String DEPLOY_CONFIG_FLAT_FOLDER_STRUCTURE = "deploy-config-repository.folder-structure.flat";
    private static final String DEPLOY_CONFIG_NESTED_FOLDER_CONFIG = "deploy-config-repository.folder-structure.configuration";

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

    private Map<String, Object> config;

    public void setConfig(Map<String, Object> config) {
        this.config = config;
    }

    public void init() {
        synchronized (projects) {
            if (repository != null) {
                return;
            }

            rulesLocation = config.get(RULES_LOCATION_CONFIG_NAME).toString();
            if (!rulesLocation.isEmpty() && !rulesLocation.endsWith("/")) {
                rulesLocation += "/";
            }
            deploymentConfigurationLocation = config.get(DEPLOYMENT_CONFIGURATION_LOCATION_CONFIG_NAME).toString();
            if (!deploymentConfigurationLocation.isEmpty() && !deploymentConfigurationLocation.endsWith("/")) {
                deploymentConfigurationLocation += "/";
            }
            boolean separateDeployConfigRepo = Boolean.parseBoolean(config.get(USE_SEPARATE_DEPLOY_CONFIG_REPO).toString());
            boolean flatProjects = Boolean.parseBoolean(config.get(PROJECTS_FLAT_FOLDER_STRUCTURE).toString());
            boolean flatDeployConfig = Boolean.parseBoolean(config.get(DEPLOY_CONFIG_FLAT_FOLDER_STRUCTURE).toString());

            try {
                repository = createRepo(RepositoryMode.DESIGN,
                    flatProjects,
                    PROJECTS_NESTED_FOLDER_CONFIG,
                    rulesLocation);

                if (!separateDeployConfigRepo) {
                    deployConfigRepository = repository;
                } else {
                    deployConfigRepository = createRepo(RepositoryMode.DEPLOY_CONFIG,
                    flatDeployConfig,
                    DEPLOY_CONFIG_NESTED_FOLDER_CONFIG,
                    deploymentConfigurationLocation);
                }

                addListener(new DesignTimeRepositoryListener() {
                    @Override
                    public void onRepositoryModified() {
                        synchronized (projects) {
                            projectsRefreshNeeded = true;
                        }
                    }
                });
            } catch (RRepositoryException e) {
                log.error("Cannot init DTR! {}", e.getMessage(), e);
                throw new IllegalStateException("Can't initialize Design Repository.", e);
            }

            RepositoryListener callback = new RepositoryListener(listeners);
            repository.setListener(callback);
            if (separateDeployConfigRepo) {
                deployConfigRepository.setListener(callback);
            }
        }
    }

    private Repository createRepo(RepositoryMode repositoryMode,
            boolean flatStructure,
            String folderConfig,
            String baseFolder) throws RRepositoryException {
        Repository repo = RepositoryFactoryInstatiator.newFactory(config, repositoryMode);
        if (!flatStructure && repo.supports().folders()) {
            // Nested folder structure is supported for FolderRepository only
            FolderRepository delegate = (FolderRepository) repo;
            String configFile = config.get(folderConfig).toString();

            MappedRepository mappedRepository = new MappedRepository();
            mappedRepository.setDelegate(delegate);
            mappedRepository.setRepositoryMode(repositoryMode);
            mappedRepository.setConfigFile(configFile);
            mappedRepository.setBaseFolder(baseFolder);
            mappedRepository.initialize();
            repo = mappedRepository;
        }

        return repo;
    }

    @Override
    public AProject createProject(String name) {
        return new AProject(getRepository(), rulesLocation + name);
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
            throw new RepositoryException("Cannot read the deploy repository", e);
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
                throw new RepositoryException("Cannot find project ''{0}''!", null, name);
            }

            AProject cached = projects.get(name);
            if (cached != null) {
                return cached;
            }

            // TODO: Seems we never reach here. Is the code below really needed?
            project = new AProject(getRepository(), rulesLocation + name);
            projects.put(project.getName(), project);
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
                            log.warn("Can't find the project '{}' which version is '{}'.", name, repoVersion);
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

        Collections.sort(result, (o1, o2) -> o1.getName().compareToIgnoreCase(o2.getName()));

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
            projects.put(project.getName(), project);
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
            return projects.containsKey(name);
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
