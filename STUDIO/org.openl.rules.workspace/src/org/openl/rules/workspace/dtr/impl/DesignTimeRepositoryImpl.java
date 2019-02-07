package org.openl.rules.workspace.dtr.impl;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.openl.rules.common.ArtefactPath;
import org.openl.rules.common.CommonVersion;
import org.openl.rules.common.ProjectException;
import org.openl.rules.project.abstraction.ADeploymentProject;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.AProjectArtefact;
import org.openl.rules.project.abstraction.ResourceTransformer;
import org.openl.rules.repository.MappedRepository;
import org.openl.rules.repository.RepositoryFactoryInstatiator;
import org.openl.rules.repository.RepositoryMode;
import org.openl.rules.repository.api.*;
import org.openl.rules.repository.exceptions.RRepositoryException;
import org.openl.rules.workspace.WorkspaceUser;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.rules.workspace.dtr.DesignTimeRepositoryListener;
import org.openl.rules.workspace.dtr.RepositoryException;
import org.openl.util.RuntimeExceptionWrapper;
import org.openl.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;

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

    private static final Pattern PROJECT_PROPERTY_PATTERN = Pattern.compile("(project\\.\\d+\\.)\\w+");

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
            repository = createRepo(RepositoryMode.DESIGN, flatProjects, PROJECTS_NESTED_FOLDER_CONFIG, rulesLocation);

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
                        projects.clear();
                        projectsVersions.clear();
                    }
                }
            });
        } catch (RRepositoryException | IOException e) {
            log.error("Cannot init DTR! {}", e.getMessage(), e);
            throw new IllegalStateException("Can't initialize Design Repository.", e);
        }

        RepositoryListener callback = new RepositoryListener(listeners);
        repository.setListener(callback);
        if (separateDeployConfigRepo) {
            deployConfigRepository.setListener(callback);
        }
    }

    private Repository createRepo(RepositoryMode repositoryMode, boolean flatStructure, String folderConfig, String baseFolder) throws
                                                                                                                          IOException,
                                                                                                                          RRepositoryException {
        Repository repo = RepositoryFactoryInstatiator.newFactory(config, repositoryMode);
        if (!flatStructure && repo instanceof FolderRepository) {
            // Nested folder structure is supported for FolderRepository only
            FolderRepository delegate = (FolderRepository) repo;
            String configFile = config.get(folderConfig).toString();
            Map<String, String> externalToInternal = readExternalToInternalMap(delegate,
                    repositoryMode,
                    configFile,
                    baseFolder
            );

            final MappedRepository mappedRepository = new MappedRepository();
            mappedRepository.setDelegate(delegate);
            mappedRepository.setExternalToInternal(externalToInternal);
            repo = mappedRepository;

            addListener(new MappedRepositoryListener(mappedRepository, repositoryMode, configFile, baseFolder));
        }

        return repo;
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

    /**
     * Load mapping from properties file.
     *
     * @param delegate   original repository
     * @param repositoryMode Repository mode: design or deploy config.
     * @param configFile properties file
     * @param baseFolder virtual base folder. WebStudio will think that projects can be found in this folder.
     * @return loaded mapping
     * @throws IOException if it was any error during operation
     */
    private Map<String, String>  readExternalToInternalMap(FolderRepository delegate,
            RepositoryMode repositoryMode, String configFile,
            String baseFolder) throws IOException {
        baseFolder = StringUtils.isBlank(baseFolder) ? "" : baseFolder.endsWith("/") ?
                                                                baseFolder : baseFolder + "/";
        Map<String, String> externalToInternal = new HashMap<>();
        FileItem fileItem = delegate.read(configFile);
        if (fileItem == null) {
            log.debug("Repository configuration file {} is not found", configFile);
            return generateExternalToInternalMap(delegate, repositoryMode, baseFolder);
        }

        Properties prop;
        try (InputStream stream = fileItem.getStream()) {
            prop = new Properties();
            prop.load(stream);
        }

        Set<String> processed = new HashSet<>();
        for (Object key : prop.keySet()) {
            String propertyName = ((String) key);

            Matcher matcher = PROJECT_PROPERTY_PATTERN.matcher(propertyName);
            if (matcher.matches()) {
                String suffix = matcher.group(1);
                if (processed.add(suffix)) {
                    String name = prop.getProperty(suffix + "name");
                    String path = prop.getProperty(suffix + "path");

                    if (name != null && path != null) {
                        if (path.endsWith("/")) {
                            path = path.substring(0, path.length() - 1);
                        }
                        String externalPath = createUniquePath(externalToInternal, baseFolder + name);

                        externalToInternal.put(externalPath, path);
                    }
                }
            }
        }

        return externalToInternal;
    }

    private String createUniquePath(Map<String, String> externalToInternal, String externalPath) {
        // If occasionally such project name exists already, add some suffix to it.
        if (externalToInternal.containsKey(externalPath)) {
            int i = 1;
            String copy = externalPath + "." + i;
            while (externalToInternal.containsKey(copy)) {
                copy = externalPath + "." + (++i);
            }
            externalPath = copy;
        }

        return externalPath;
    }

    /**
     * Detect existing projects and Deploy Configurations based on rules.xml and {@link ArtefactProperties#DESCRIPTORS_FILE}.
     * If there are several projects with same name, suffix will be added to them
     *
     * @param delegate       repository to detect projects
     * @param repositoryMode repository mode. If design repository, rules.xml will be searched, otherwise {@link ArtefactProperties#DESCRIPTORS_FILE}
     * @param baseFolder     virtual base folder. WebStudio will think that projects can be found in this folder.
     * @return generated mapping
     */
    private Map<String, String> generateExternalToInternalMap(FolderRepository delegate,
            RepositoryMode repositoryMode,
            String baseFolder) throws IOException {
        Map<String, String> externalToInternal = new HashMap<>();
        List<FileData> allFiles = delegate.list("");
        for (FileData fileData : allFiles) {
            String fullName = fileData.getName();
            String[] nameParts = fullName.split("/");
            if (nameParts.length == 0) {
                continue;
            }
            String fileName = nameParts[nameParts.length - 1];
            if (repositoryMode == RepositoryMode.DESIGN) {
                if ("rules.xml".equals(fileName)) {
                    FileItem fileItem = delegate.read(fullName);
                    try (InputStream stream = fileItem.getStream()) {
                        String projectName = getProjectName(stream);
                        String externalPath = createUniquePath(externalToInternal, baseFolder + projectName);

                        int cutSize = "rules.xml".length() + (nameParts.length > 1 ? 1 : 0); // Exclude "/" if exist
                        String path = fullName.substring(0, fullName.length() - cutSize);
                        externalToInternal.put(externalPath, path);
                    }
                }
            } else if (repositoryMode == RepositoryMode.DEPLOY_CONFIG) {
                if (ArtefactProperties.DESCRIPTORS_FILE.equals(fileName)) {
                    if (nameParts.length < 2) {
                        continue;
                    }

                    String deployConfigName = nameParts[nameParts.length - 2];
                    String externalPath = createUniquePath(externalToInternal, baseFolder + deployConfigName);
                    int cutSize = ArtefactProperties.DESCRIPTORS_FILE.length() + 1; // Exclude "/"
                    String path = fullName.substring(0, fullName.length() - cutSize);
                    externalToInternal.put(externalPath, path);
                }
            }
        }

        return externalToInternal;
    }

    private String getProjectName(InputStream inputStream) {
        try {
            InputSource inputSource = new InputSource(inputStream);
            XPathFactory factory = XPathFactory.newInstance();
            XPath xPath = factory.newXPath();
            XPathExpression xPathExpression = xPath.compile("/project/name");
            return xPathExpression.evaluate(inputSource);
        } catch (XPathExpressionException e) {
            return null;
        }
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

    private class MappedRepositoryListener implements DesignTimeRepositoryListener {
        private final MappedRepository mappedRepository;
        private final RepositoryMode repositoryMode;
        private final String configFile;
        private final String baseFolder;
        private Date lastModified;

        public MappedRepositoryListener(MappedRepository mappedRepository,
                RepositoryMode repositoryMode,
                String configFile,
                String baseFolder) {
            this.mappedRepository = mappedRepository;
            this.repositoryMode = repositoryMode;
            this.configFile = configFile;
            this.baseFolder = baseFolder;
        }

        @Override
        public void onRepositoryModified() {
            try {
                FolderRepository delegate = mappedRepository.getDelegate();
                FileData data = delegate.check(configFile);
                if (data == null) {
                    log.debug("Repository configuration file {} is not found", configFile);
                    Map<String, String> mapping = generateExternalToInternalMap(delegate,
                            repositoryMode,
                            baseFolder);
                    mappedRepository.setExternalToInternal(mapping);
                    return;
                }

                // No need to reload mapping from config if it's not modified.
                Date modifiedAt = data.getModifiedAt();
                if (lastModified == null || modifiedAt.after(lastModified)) {
                    lastModified = modifiedAt;

                    Map<String, String> mapping = readExternalToInternalMap(delegate,
                            repositoryMode,
                            configFile, baseFolder
                    );
                    mappedRepository.setExternalToInternal(mapping);
                }
            } catch (Exception e) {
                log.warn(e.getMessage(), e);
            }
        }
    }
}
