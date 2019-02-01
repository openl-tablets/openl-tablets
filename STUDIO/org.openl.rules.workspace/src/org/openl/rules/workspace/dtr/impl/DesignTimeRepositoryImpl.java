package org.openl.rules.workspace.dtr.impl;

import org.openl.rules.common.ArtefactPath;
import org.openl.rules.common.CommonVersion;
import org.openl.rules.common.ProjectException;
import org.openl.rules.project.abstraction.ADeploymentProject;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.AProjectArtefact;
import org.openl.rules.project.abstraction.ResourceTransformer;
import org.openl.rules.repository.RepositoryFactoryInstatiator;
import org.openl.rules.repository.RepositoryMode;
import org.openl.rules.repository.api.*;
import org.openl.rules.repository.exceptions.RRepositoryException;
import org.openl.rules.workspace.WorkspaceUser;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.rules.workspace.dtr.DesignTimeRepositoryListener;
import org.openl.rules.workspace.dtr.RepositoryException;
import org.openl.util.RuntimeExceptionWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author Aleh Bykhavets
 */
public class DesignTimeRepositoryImpl implements DesignTimeRepository {
    public static final String USE_SEPARATE_DEPLOY_CONFIG_REPO = "deploy-config-repository.separate-repository";
    private final Logger log = LoggerFactory.getLogger(DesignTimeRepositoryImpl.class);

    private static final String RULES_LOCATION_CONFIG_NAME = "design-repository.base.path";
    private static final String DEPLOYMENT_CONFIGURATION_LOCATION_CONFIG_NAME = "deploy-config-repository.base.path";

    private Repository repository;
    private Repository deployConfigRepository;
    private String rulesLocation;
    private String deploymentConfigurationLocation;
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

    private synchronized void init() {
        if (repository != null) {
            return;
        }

        boolean separateDeployConfigRepo = Boolean.parseBoolean(config.get(USE_SEPARATE_DEPLOY_CONFIG_REPO).toString());
        try {
            repository = RepositoryFactoryInstatiator.newFactory(config, RepositoryMode.DESIGN);
            if (!separateDeployConfigRepo) {
                deployConfigRepository = repository;
            } else {
                deployConfigRepository = RepositoryFactoryInstatiator.newFactory(config, RepositoryMode.DEPLOY_CONFIG);
            }

            rulesLocation = config.get(RULES_LOCATION_CONFIG_NAME).toString();

            deploymentConfigurationLocation = config.get(DEPLOYMENT_CONFIGURATION_LOCATION_CONFIG_NAME).toString();

            addListener(new DesignTimeRepositoryListener() {
                @Override
                public void onRepositoryModified() {
                    synchronized (projects) {
                        projects.clear();
                        projectsVersions.clear();
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

    public void copyProject(AProject project, String name, WorkspaceUser user, ResourceTransformer resourceTransformer) throws ProjectException {
        if (hasProject(name)) {
            throw new ProjectException("Project ''{0}'' is already exist in the repository!", null, name);
        }

        try {
            AProject newProject = new AProject(getRepository(), rulesLocation + name);

            newProject.setResourceTransformer(resourceTransformer);
            newProject.update(project, user);
            newProject.setResourceTransformer(null);
        } catch (RRepositoryException e) {
            throw new RepositoryException("Failed to create project ''{0}''!", e, name);
        } catch (Exception e) {
            throw new RepositoryException("Failed to copy project ''{0}''!", e, name);
        } finally {
            synchronized (projects) {
                // invalidate cache (rules projects)
                projects.remove(name);
                projectsVersions.clear();
            }
        }
    }

    public AProject createProject(String name) {
        return new AProject(getRepository(), rulesLocation + name);
    }

    public AProjectArtefact getArtefactByPath(ArtefactPath artefactPath) throws ProjectException {
        String projectName = artefactPath.segment(0);
        AProject ralProject = getProject(projectName);

        ArtefactPath pathInProject = artefactPath.withoutFirstSegment();
        return ralProject.getArtefactByPath(pathInProject);
    }

    public ADeploymentProject.Builder createDeploymentConfigurationBuilder(String name) {
        return new ADeploymentProject.Builder(getDeployConfigRepository(), deploymentConfigurationLocation + name);
    }

    public List<ADeploymentProject> getDDProjects() throws RepositoryException {
        LinkedList<ADeploymentProject> result = new LinkedList<>();
        Repository repository = getDeployConfigRepository();

        Collection<FileData> fileDatas;
        try {
            String path = deploymentConfigurationLocation;
            if (repository instanceof FolderRepository) {
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

    public AProject getProject(String name) throws RepositoryException {
        AProject project;
        synchronized (projects) {
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

    public AProject getProject(String name, CommonVersion version) {
        String key = String.format("%s:%s", name, version.getVersionName());
        AProject project = projectsVersions.get(key);
        if (project == null) {
            project = new AProject(getRepository(), rulesLocation + name, version.getVersionName());
            projectsVersions.put(key, project);
        }
        return project;
    }

    public Collection<AProject> getProjects() {
        List<AProject> result = new LinkedList<>();

        Collection<FileData> fileDatas;
        Repository repository = getRepository();
        try {
            String path = rulesLocation;
            if (repository instanceof FolderRepository) {
                fileDatas = ((FolderRepository) repository).listFolders(path);
            } else {
                fileDatas = repository.list(path);
            }
        } catch (IOException ex) {
            throw RuntimeExceptionWrapper.wrap(ex);
        }
        synchronized (projects) {
            projects.clear();
            projectsVersions.clear();
            for (FileData fileData : fileDatas) {
                AProject project = new AProject(repository, fileData);
                // get from the repository
                result.add(project);
                projects.put(project.getName(), project);
            }
        }
        return result;
    }

    public boolean hasDDProject(String name) {
        try {
            return getDeployConfigRepository().check(deploymentConfigurationLocation + name) != null;
        } catch (IOException ex) {
            return false;
        }
    }

    public boolean hasProject(String name) {
        synchronized (projects) {
            return projects.containsKey(name);
        }
    }

    // --- private

    public void addListener(DesignTimeRepositoryListener listener) {
        listeners.add(listener);
    }

    public void removeListener(DesignTimeRepositoryListener listener) {
        listeners.remove(listener);
    }

    public List<DesignTimeRepositoryListener> getListeners() {
        return listeners;
    }

    /**
     * destroy-method
     */
    public synchronized void destroy() throws Exception {
        if (repository != null) {
            repository.setListener(null);
            if (repository instanceof Closeable) {
                ((Closeable) repository).close();
            }
            repository = null;
        }
        if (deployConfigRepository != null) {
            deployConfigRepository.setListener(null);
            if (deployConfigRepository instanceof Closeable) {
                ((Closeable) deployConfigRepository).close();
            }
        }

        synchronized (projects) {
            projects.clear();
            projectsVersions.clear();
        }
    }

    @Override
    public Repository getRepository() {
        if (repository == null) {
            init();
        }
        return repository;
    }

    private Repository getDeployConfigRepository() {
        if (deployConfigRepository == null) {
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
