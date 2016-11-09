package org.openl.rules.workspace.dtr.impl;

import org.openl.config.ConfigPropertyString;
import org.openl.config.ConfigSet;
import org.openl.rules.common.ArtefactPath;
import org.openl.rules.common.CommonVersion;
import org.openl.rules.common.ProjectException;
import org.openl.rules.project.abstraction.ADeploymentProject;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.AProjectArtefact;
import org.openl.rules.project.abstraction.ResourceTransformer;
import org.openl.rules.repository.RepositoryFactoryInstatiator;
import org.openl.rules.repository.api.*;
import org.openl.rules.repository.exceptions.RRepositoryException;
import org.openl.rules.workspace.WorkspaceUser;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.rules.workspace.dtr.DesignTimeRepositoryListener;
import org.openl.rules.workspace.dtr.RepositoryException;
import org.openl.util.IOUtils;
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
    private final Logger log = LoggerFactory.getLogger(DesignTimeRepositoryImpl.class);

    private static final String RULES_LOCATION_CONFIG_NAME = "design-repository.rules.path";
    private static final String DEPLOYMENT_CONFIGURATION_LOCATION_CONFIG_NAME = "design-repository.deployments.path";

    private Repository repository;
    private String rulesLocation;
    private String deploymentConfigurationLocation;
    /**
     * Project Cache
     */
    private HashMap<String, AProject> projects = new HashMap<String, AProject>();

    private final List<DesignTimeRepositoryListener> listeners = new ArrayList<DesignTimeRepositoryListener>();

    private Map<String, Object> config;

    public void setConfig(Map<String, Object> config) {
        this.config = config;
    }

    private synchronized void init() {
        if (repository != null) {
            return;
        }


        try {
            repository = createConnection(config);

            Object path;
            path = config.get(RULES_LOCATION_CONFIG_NAME);
            rulesLocation = preparePathPrefix(path == null ? "DESIGN/rules" : path.toString());

            path = config.get(DEPLOYMENT_CONFIGURATION_LOCATION_CONFIG_NAME);
            deploymentConfigurationLocation = preparePathPrefix(path == null ? "DESIGN/deployments" : path.toString());

            addListener(new DesignTimeRepositoryListener() {
                @Override
                public void onRepositoryModified() {
                    projects.clear();
                }
            });
        } catch (RRepositoryException e) {
            log.error("Cannot init DTR! {}", e.getMessage(), e);
            throw new IllegalStateException("Can't initialize Design Repository.", e);
        }

        repository.setListener(new RepositoryListener(listeners));
    }

    private String preparePathPrefix(String path) {
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        return path;
    }

    public Repository createConnection(Map<String, Object> properties) throws RRepositoryException {
        ConfigSet config = new ConfigSet();
        config.addProperties(properties);

        // default value is <code>null</code> -- fail first
        ConfigPropertyString confRepositoryFactoryClass = new ConfigPropertyString("design-repository.factory", null);
        config.updateProperty(confRepositoryFactoryClass);
        String className = confRepositoryFactoryClass.getValue();

        return RepositoryFactoryInstatiator.newFactory(className, config, true);
    }

    public void copyDDProject(ADeploymentProject project, String name, WorkspaceUser user)
            throws ProjectException {
        ADeploymentProject newProject = new ADeploymentProject(null,
                getRepository(),
                deploymentConfigurationLocation + "/" + name,
                null);
        newProject.update(project, user);
    }

    public void copyProject(AProject project, String name, WorkspaceUser user, ResourceTransformer resourceTransformer) throws ProjectException {
        if (hasProject(name)) {
            throw new ProjectException("Project ''{0}'' is already exist in the repository!", null, name);
        }

        try {
            AProject newProject = new AProject(getRepository(), rulesLocation + "/" + name);

            newProject.setResourceTransformer(resourceTransformer);
            newProject.update(project, user);
            newProject.setResourceTransformer(null);
        } catch (RRepositoryException e) {
            throw new RepositoryException("Failed to create project ''{0}''!", e, name);
        } catch (Exception e) {
            throw new RepositoryException("Failed to copy project ''{0}''!", e, name);
        } finally {
            // invalidate cache (rules projects)
            projects.remove(name);
        }
    }

    public ADeploymentProject createDDProject(String name) throws RepositoryException {
        return new ADeploymentProject(null, getRepository(), deploymentConfigurationLocation + "/" + name, null);
    }

    public AProject createProject(String name) throws RepositoryException {
        return new AProject(getRepository(), rulesLocation + "/" + name);
    }

    public AProjectArtefact getArtefactByPath(ArtefactPath artefactPath) throws ProjectException {
        String projectName = artefactPath.segment(0);
        AProject ralProject = getProject(projectName);

        ArtefactPath pathInProject = artefactPath.withoutFirstSegment();
        return ralProject.getArtefactByPath(pathInProject);
    }

    public ADeploymentProject getDDProject(String name) throws RepositoryException {
        return new ADeploymentProject(null, getRepository(), deploymentConfigurationLocation + "/" + name, null);
    }

    public ADeploymentProject getDDProject(String name, CommonVersion version) throws RepositoryException {
        return new ADeploymentProject(null, getRepository(), deploymentConfigurationLocation + "/" + name, version.getVersionName());
    }

    public List<ADeploymentProject> getDDProjects() throws RepositoryException {
        LinkedList<ADeploymentProject> result = new LinkedList<ADeploymentProject>();

        Collection<FileData> fileDatas = getRepository().list(deploymentConfigurationLocation);
        for (FileData fileData : fileDatas) {
            result.add(new ADeploymentProject(null, getRepository(), fileData));
        }
        return result;
    }

    public AProject getProject(String name) throws RepositoryException {
        if (!hasProject(name)) {
            throw new RepositoryException("Cannot find project ''{0}''!", null, name);
        }

        AProject cached = projects.get(name);
        if (cached != null) {
            return cached;
        }

        AProject project = new AProject(getRepository(), rulesLocation + "/" + name);
        projects.put(project.getName(), project);
        return project;
    }

    public AProject getProject(String name, CommonVersion version) throws RepositoryException {
        return new AProject(getRepository(), rulesLocation + "/" + name, version.getVersionName());
    }

    public Collection<AProject> getProjects() {
        List<AProject> result = new LinkedList<AProject>();

        Collection<FileData> fileDatas = getRepository().list(rulesLocation);
        projects.clear();
        for (FileData fileData : fileDatas) {
            AProject project = new AProject(getRepository(), fileData);
            // get from the repository
            result.add(project);
            projects.put(project.getName(), project);
        }
        return result;
    }

    public boolean hasDDProject(String name) {
        FileItem item;
        try {
            item = getRepository().read(deploymentConfigurationLocation + "/" + name);
        } catch (IOException ex) {
            return false;
        }
        if (item != null) {
            IOUtils.closeQuietly(item.getStream());
            return true;
        }
        return false;
    }

    public boolean hasProject(String name) {
        return projects.containsKey(name);
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
        projects.clear();
    }

    @Override
    public Repository getRepository() {
        if (repository == null) {
            init();
        }
        return repository;
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
