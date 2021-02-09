package org.openl.rules.workspace.dtr.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.openl.rules.repository.api.AdditionalData;
import org.openl.rules.repository.api.ArtefactProperties;
import org.openl.rules.repository.api.BranchRepository;
import org.openl.rules.repository.api.ChangesetType;
import org.openl.rules.repository.api.ConflictResolveData;
import org.openl.rules.repository.api.Features;
import org.openl.rules.repository.api.FeaturesBuilder;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.FileItem;
import org.openl.rules.repository.api.FolderItem;
import org.openl.rules.repository.api.FolderMapper;
import org.openl.rules.repository.api.FolderRepository;
import org.openl.rules.repository.api.Listener;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.repository.api.RepositorySettings;
import org.openl.util.IOUtils;
import org.openl.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.TypeDescription;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.representer.Representer;

public class MappedRepository implements FolderRepository, BranchRepository, Closeable, FolderMapper {
    private static final Logger log = LoggerFactory.getLogger(MappedRepository.class);
    private static final String SEPARATOR = ":";
    private final MessageDigest digest;

    private FolderRepository delegate;

    private volatile ProjectIndex externalToInternal = new ProjectIndex();

    private String configFile;
    private String baseFolder;
    private RepositorySettings repositorySettings;
    private Date settingsSyncDate = new Date();

    public static Repository create(FolderRepository delegate,
            String baseFolder,
            RepositorySettings repositorySettings) throws IOException {
        MappedRepository mappedRepository = null;
        try {
            mappedRepository = new MappedRepository();
            mappedRepository.setDelegate(delegate);
            mappedRepository.setConfigFile(delegate.getId() + "/openl-projects.yaml");
            mappedRepository.setBaseFolder(baseFolder);
            mappedRepository.setRepositorySettings(repositorySettings);
            mappedRepository.initialize();
        } catch (Exception e) {
            // If exception is thrown, we must close repository in this method and rethrow exception.
            // If no exception, repository will be closed later.
            if (mappedRepository != null) {
                IOUtils.closeQuietly(mappedRepository);
            }
            throw e;
        }
        return mappedRepository;
    }

    private MappedRepository() {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            log.error(e.getMessage(), e);
            digest = null;
        }
        this.digest = digest;
    }

    @Override
    public FolderRepository getDelegate() {
        return delegate;
    }

    public void setDelegate(FolderRepository delegate) {
        this.delegate = delegate;
    }

    private void setConfigFile(String configFile) {
        this.configFile = configFile;
    }

    private void setBaseFolder(String baseFolder) {
        this.baseFolder = baseFolder;
    }

    private void setRepositorySettings(RepositorySettings repositorySettings) {
        this.repositorySettings = repositorySettings;
    }

    @Override
    public void close() throws IOException {
        externalToInternal = new ProjectIndex();

        if (delegate instanceof Closeable) {
            ((Closeable) delegate).close();
        } else if (delegate != null) {
            try {
                delegate.close();
            } catch (Exception e) {
                throw new IOException(e);
            }
        }
    }

    @Override
    public String getId() {
        return delegate.getId();
    }

    @Override
    public String getName() {
        return delegate.getName();
    }

    @Override
    public List<FileData> list(String path) throws IOException {
        ProjectIndex mapping = getUpToDateMapping(true);

        List<FileData> internal = new ArrayList<>();
        for (ProjectInfo project : mapping.getProjects()) {
            String external = baseFolder + getMappedName(project);
            if (external.startsWith(path)) {
                internal.addAll(delegate.list(project.getPath() + "/"));
            } else if (path.startsWith(external + "/")) {
                internal.addAll(delegate.list(toInternal(mapping, path)));
            }
        }

        return toExternal(mapping, internal);
    }

    @Override
    public FileData check(String name) throws IOException {
        ProjectIndex mapping = getUpToDateMapping(true);
        FileData check = delegate.check(toInternal(mapping, name));
        if (check != null && delegate.supports().versions()) {
            Optional<ProjectInfo> project = externalToInternal.getProjects()
                .stream()
                .filter(p -> name.equals(baseFolder + getMappedName(p)))
                .findFirst();
            check.setDeleted(project.isPresent() && project.get().isArchived());
        }
        return toExternal(mapping, check);
    }

    @Override
    public FileItem read(String name) throws IOException {
        ProjectIndex mapping = getUpToDateMapping(true);
        return toExternal(mapping, delegate.read(toInternal(mapping, name)));
    }

    @Override
    public FileData save(FileData data, InputStream stream) throws IOException {
        ProjectIndex mapping = getUpToDateMapping(true);
        return toExternal(mapping, delegate.save(toInternal(mapping, data), stream));
    }

    @Override
    public List<FileData> save(List<FileItem> fileItems) throws IOException {
        ProjectIndex mapping = getUpToDateMapping(true);
        List<FileItem> fileItemsInternal = new ArrayList<>(fileItems.size());
        for (FileItem fi : fileItems) {
            fileItemsInternal.add(new FileItem(toInternal(mapping, fi.getData()), fi.getStream()));
        }
        List<FileData> result = delegate.save(fileItemsInternal);

        return toExternal(mapping, result);
    }

    @Override
    public boolean delete(FileData data) throws IOException {
        if (delegate.supports().versions()) {
            repositorySettings.lock(configFile);
            try {
                ProjectIndex projectIndex = getUpToDateMapping(false);
                Optional<ProjectInfo> projectInfo = findProject(projectIndex, data);
                if (projectInfo.isPresent()) {
                    projectInfo.get().setArchived(true);
                    saveProjectIndex(projectIndex);
                    return true;
                }
            } finally {
                repositorySettings.unlock(configFile);
            }
        }

        ProjectIndex mapping = getUpToDateMapping(true);
        return delegate.delete(toInternal(mapping, data));
    }

    @Override
    public void setListener(final Listener callback) {
        delegate.setListener(() -> {
            try {
                repositorySettings.lock(configFile);
                try {
                    refreshMapping();

                    ProjectIndex projectIndex = externalToInternal.copy();
                    boolean modified = syncProjectIndex(delegate, projectIndex);
                    if (modified) {
                        saveProjectIndex(projectIndex);
                    }
                } catch (Exception e) {
                    log.warn(e.getMessage(), e);
                } finally {
                    repositorySettings.unlock(configFile);
                }
            } catch (IOException e) {
                log.warn("Skip project index updating because thread was interrupted.", e);
            }

            if (callback != null) {
                callback.onChange();
            }
        });
    }

    @Override
    public List<FileData> listHistory(String name) throws IOException {
        ProjectIndex mapping = getUpToDateMapping(true);
        return toExternal(mapping, delegate.listHistory(toInternal(mapping, name)));
    }

    @Override
    public FileData checkHistory(String name, String version) throws IOException {
        ProjectIndex mapping = getUpToDateMapping(true);
        return toExternal(mapping, delegate.checkHistory(toInternal(mapping, name), version));
    }

    @Override
    public FileItem readHistory(String name, String version) throws IOException {
        ProjectIndex mapping = getUpToDateMapping(true);
        return toExternal(mapping, delegate.readHistory(toInternal(mapping, name), version));
    }

    @Override
    public boolean deleteHistory(FileData data) throws IOException {
        ProjectIndex mapping = getUpToDateMapping(true);

        FileData internalToDelete = toInternal(mapping, data);
        if (data.getVersion() == null) {
            try {
                removeMapping(data.getName());

                // Use mapping before modification
                return delegate.deleteHistory(internalToDelete);
            } catch (IOException | RuntimeException e) {
                refreshMappingWithLock();
                throw e;
            }
        } else {
            if (delegate.supports().versions()) {
                repositorySettings.lock(configFile);
                try {
                    ProjectIndex projectIndex = getUpToDateMapping(false);
                    Optional<ProjectInfo> project = findProject(projectIndex, data);
                    if (project.isPresent()) {
                        ProjectInfo projectInfo = project.get();
                        if (projectInfo.isArchived()) {
                            projectInfo.setArchived(false);
                            saveProjectIndex(projectIndex);
                            return true;
                        }
                    }
                } finally {
                    repositorySettings.unlock(configFile);
                }
            }
            return delegate.deleteHistory(internalToDelete);
        }
    }

    @Override
    public FileData copyHistory(String srcName, FileData destData, String version) throws IOException {
        ProjectIndex mapping;
        if (isUpdateConfigNeeded(destData)) {
            mapping = updateConfigFile(destData);
        } else {
            mapping = getUpToDateMapping(true);
        }

        return toExternal(mapping,
            delegate.copyHistory(toInternal(mapping, srcName), toInternal(mapping, destData), version));
    }

    @Override
    public List<FileData> listFolders(String path) throws IOException {
        ProjectIndex mapping = getUpToDateMapping(true);

        List<FileData> internal = new ArrayList<>();
        for (ProjectInfo project : mapping.getProjects()) {
            String external = baseFolder + getMappedName(project);
            if (external.startsWith(path) && !external.substring(path.length()).contains("/")) {
                // "external" is direct child of "path"
                FileData data = delegate.check(project.getPath());
                if (data == null) {
                    // It can be intermediate state: project is added to index, but not still committed.
                    // Or project could be removed from repository, but index isn't updated. Will be updated later.
                    log.debug("Project {} is not found.", project.getPath());
                } else {
                    if (delegate.supports().versions()) {
                        data.setDeleted(project.isArchived());
                    }
                    internal.add(data);
                }
            }
        }

        return toExternal(mapping, internal);
    }

    @Override
    public List<FileData> listFiles(String path, String version) throws IOException {
        ProjectIndex mapping = getUpToDateMapping(true);
        return toExternal(mapping, delegate.listFiles(toInternal(mapping, path), version));
    }

    @Override
    public FileData save(FileData folderData,
            Iterable<FileItem> files,
            ChangesetType changesetType) throws IOException {
        ProjectIndex mapping;
        if (isUpdateConfigNeeded(folderData)) {
            mapping = updateConfigFile(folderData);
        } else {
            mapping = getUpToDateMapping(true);
        }
        return toExternal(mapping,
            delegate.save(toInternal(mapping, folderData), toInternal(mapping, folderData, files), changesetType));
    }

    @Override
    public List<FileData> save(List<FolderItem> folderItems, ChangesetType changesetType) throws IOException {
        if (folderItems.isEmpty()) {
            return Collections.emptyList();
        }

        if (folderItems.get(0).getData().getAdditionalData(FileMappingData.class) != null) {
            throw new UnsupportedOperationException("File name mapping is not supported.");
        }
        ProjectIndex mapping = getUpToDateMapping(true);

        List<FolderItem> folderItemsInternal = new ArrayList<>(folderItems.size());
        for (FolderItem fi : folderItems) {
            folderItemsInternal
                .add(new FolderItem(toInternal(mapping, fi.getData()), toInternal(mapping, null, fi.getFiles())));
        }
        List<FileData> result = delegate.save(folderItemsInternal, changesetType);

        return toExternal(mapping, result);
    }

    @Override
    public Features supports() {
        return new FeaturesBuilder(delegate).setVersions(delegate.supports().versions())
            .setMappedFolders(true)
            .setSupportsUniqueFileId(delegate.supports().uniqueFileId())
            .build();
    }

    @Override
    public void merge(String branchFrom, String author, ConflictResolveData conflictResolveData) throws IOException {
        ((BranchRepository) delegate).merge(branchFrom, author, conflictResolveData);
    }

    @Override
    public String getBaseBranch() {
        return delegate.supports().branches() ? ((BranchRepository) delegate).getBaseBranch() : null;
    }

    @Override
    public void pull(String author) throws IOException {
        ((BranchRepository) delegate).pull(author);
    }

    @Override
    public boolean isMergedInto(String from, String to) throws IOException {
        return ((BranchRepository) delegate).isMergedInto(from, to);
    }

    @Override
    public String getBranch() {
        return ((BranchRepository) delegate).getBranch();
    }

    @Override
    public void createBranch(String projectPath, String branch) throws IOException {
        ProjectIndex mapping = getUpToDateMapping(true);
        ((BranchRepository) delegate).createBranch(toInternal(mapping, projectPath), branch);
    }

    @Override
    public void deleteBranch(String projectPath, String branch) throws IOException {
        ProjectIndex mapping = getUpToDateMapping(true);
        ((BranchRepository) delegate).deleteBranch(toInternal(mapping, projectPath), branch);
    }

    @Override
    public List<String> getBranches(String projectPath) throws IOException {
        String internalPath = null;
        if (projectPath != null) {
            ProjectIndex mapping = getUpToDateMapping(true);
            internalPath = toInternal(mapping, projectPath);
        }
        return ((BranchRepository) delegate).getBranches(internalPath);
    }

    @Override
    public BranchRepository forBranch(String branch) throws IOException {
        BranchRepository delegateForBranch = ((BranchRepository) delegate).forBranch(branch);

        MappedRepository mappedRepository = null;
        try {
            mappedRepository = new MappedRepository();
            mappedRepository.setDelegate((FolderRepository) delegateForBranch);
            mappedRepository.setConfigFile(configFile);
            mappedRepository.setBaseFolder(baseFolder);
            mappedRepository.setRepositorySettings(repositorySettings);
            mappedRepository.initialize();
        } catch (Exception e) {
            // If exception is thrown, we must close repository in this method and rethrow exception.
            // If no exception, repository will be closed later.
            if (mappedRepository != null) {
                // We don't close delegate in forBranch() method for now, because it can break main branch repository
                // (for delegate).
                mappedRepository.setDelegate(null);
                IOUtils.closeQuietly(mappedRepository);
            }
            throw e;
        }

        return mappedRepository;
    }

    @Override
    public void addMapping(String internal) throws IOException {
        repositorySettings.lock(configFile);
        try {
            if (internal.endsWith("/")) {
                internal = internal.substring(0, internal.length() - 1);
            }

            String fullName = internal + "/rules.xml";
            FileData fileData = delegate.check(fullName);
            ProjectInfo project;
            if (fileData != null) {
                FileItem descriptorItem = delegate.read(fullName);
                try (InputStream is = descriptorItem.getStream()) {
                    project = new ProjectInfo(getProjectName(is), internal);
                }
            } else {
                project = new ProjectInfo(internal.substring(internal.lastIndexOf('/') + 1), internal);
            }
            ProjectIndex externalToInternal = getUpToDateMapping(false);
            List<ProjectInfo> projectsWithSameName = externalToInternal.getProjects()
                .stream()
                .filter(p -> p.getName().equals(project.getName()))
                .collect(Collectors.toList());
            if (!projectsWithSameName.isEmpty()) {
                if (projectsWithSameName.stream().anyMatch(p -> p.getPath().equals(project.getPath()))) {
                    throw new IOException("Project \"" + project.getName() + "\" with path \"" + project
                        .getPath() + "\" is already imported.");
                }
            }
            externalToInternal.getProjects().add(project);

            saveProjectIndex(externalToInternal);
        } finally {
            repositorySettings.unlock(configFile);
        }
    }

    @Override
    public void removeMapping(String external) throws IOException {
        repositorySettings.lock(configFile);
        try {
            ProjectIndex externalToInternal = getUpToDateMapping(false);
            externalToInternal.getProjects()
                .removeIf(projectInfo -> external.equals(baseFolder + getMappedName(projectInfo)));

            saveProjectIndex(externalToInternal);
        } finally {
            repositorySettings.unlock(configFile);
        }
    }

    @Override
    public void addFileData(FileData fileData) throws IOException {
        updateConfigFile(fileData);
        FileMappingData additionalData = fileData.getAdditionalData(FileMappingData.class);
        fileData.setName(toExternal(getUpToDateMapping(true), additionalData.getInternalPath()));
    }

    private Optional<ProjectInfo> findProject(ProjectIndex projectIndex, FileData data) {
        FileMappingData mappingData = data.getAdditionalData(FileMappingData.class);
        if (mappingData != null) {
            String internalPath = mappingData.getInternalPath();
            return projectIndex.getProjects().stream().filter(p -> internalPath.equals(p.getPath())).findFirst();
        } else {
            String name = data.getName().startsWith(baseFolder) ? data.getName().substring(baseFolder.length())
                                                                : data.getName();
            return projectIndex.getProjects().stream().filter(p -> name.equals(getMappedName(p))).findFirst();
        }
    }

    private void saveProjectIndex(ProjectIndex projectIndex) throws IOException {
        ByteArrayInputStream configInputStream = getStreamFromProperties(projectIndex);

        FileData configData = new FileData();
        configData.setName(configFile);
        configData.setAuthor(getClass().getName());
        configData.setComment("Update mapping");
        repositorySettings.getRepository().save(configData, configInputStream);
        this.externalToInternal = projectIndex;
    }

    /**
     * Check if mapping should be refreshed and if should, read it from file.
     *
     * @param withLock if true and refresh is needed then lock file will be created during reading. If false, lock
     *            should be managed outside. If refresh isn't needed, lock file will not be created, this flag doesn't
     *            matter.
     */
    private ProjectIndex getUpToDateMapping(boolean withLock) throws IOException {
        boolean modified = !repositorySettings.getSyncDate().equals(settingsSyncDate);
        if (!modified) {
            try {
                FileData fileData = repositorySettings.getRepository().check(configFile);
                modified = fileData != null && settingsSyncDate.before(fileData.getModifiedAt());
            } catch (IOException e) {
                // Some IO error. Skip refreshing, do it next time.
                log.warn(e.getMessage(), e);
            }
        }

        if (modified) {
            if (withLock) {
                refreshMappingWithLock();
            } else {
                refreshMapping();
            }
        }

        return externalToInternal.copy();
    }

    private Iterable<FileItem> toInternal(final ProjectIndex mapping,
            FileData folderData,
            final Iterable<FileItem> files) {
        return () -> new Iterator<FileItem>() {
            private final Iterator<FileItem> delegate = files.iterator();

            @Override
            public boolean hasNext() {
                return delegate.hasNext();
            }

            @Override
            public FileItem next() {
                FileItem external = delegate.next();
                FileData data = external.getData();
                String name;
                if (folderData != null && folderData.getAdditionalData(FileMappingData.class) != null) {
                    String path = data.getName();
                    if (path.startsWith(folderData.getName())) {
                        String folderPath = folderData.getAdditionalData(FileMappingData.class).getInternalPath();
                        path = folderPath + path.substring(folderData.getName().length());
                    }
                    name = path;
                } else {
                    name = toInternalPath(mapping, data);
                }
                data.setName(name);
                return new FileItem(data, external.getStream());
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException("Remove is not supported");
            }
        };
    }

    private FileData toInternal(final ProjectIndex externalToInternal, FileData data) {
        FileData copy = new FileData();
        copy.setVersion(data.getVersion());
        copy.setAuthor(data.getAuthor());
        copy.setComment(data.getComment());
        copy.setSize(data.getSize());
        copy.setDeleted(data.isDeleted());
        copy.setName(toInternalPath(externalToInternal, data));

        for (AdditionalData<?> value : data.getAdditionalData().values()) {
            copy.addAdditionalData(value.convertPaths(oldPath -> toInternal(externalToInternal, oldPath)));
        }

        return copy;
    }

    private String toInternal(ProjectIndex externalToInternal, String externalPath) {
        if (externalPath == null) {
            return null;
        }
        for (ProjectInfo project : externalToInternal.getProjects()) {
            String externalBase = baseFolder + getMappedName(project);
            if (externalPath.equals(externalBase) || externalPath.startsWith(externalBase + "/")) {
                return project.getPath() + externalPath.substring(externalBase.length());
            }
        }

        log.warn("Mapping for external folder '{}' is not found. Use it as is.", externalPath);
        return externalPath;
    }

    private String toInternalPath(ProjectIndex externalToInternal, FileData data) {
        FileMappingData mappingData = data.getAdditionalData(FileMappingData.class);
        if (mappingData != null) {
            return mappingData.getInternalPath();
        }

        return toInternal(externalToInternal, data.getName());
    }

    private List<FileData> toExternal(ProjectIndex externalToInternal, List<FileData> internal) {
        List<FileData> external = new ArrayList<>(internal.size());

        for (FileData data : internal) {
            external.add(toExternal(externalToInternal, data));
        }

        return external;
    }

    private FileItem toExternal(ProjectIndex externalToInternal, FileItem internal) {
        if (internal == null) {
            return null;
        }
        return new FileItem(toExternal(externalToInternal, internal.getData()), internal.getStream());
    }

    private FileData toExternal(ProjectIndex externalToInternal, FileData data) {
        if (data == null) {
            return null;
        }

        String name = toExternal(externalToInternal, data.getName());
        data.addAdditionalData(new FileMappingData(name, data.getName()));
        data.setName(name);

        Optional<ProjectInfo> project = externalToInternal.getProjects()
            .stream()
            .filter(p -> name.equals(baseFolder + getMappedName(p)))
            .findFirst();
        data.setDeleted(project.isPresent() && project.get().isArchived());

        return data;
    }

    private String toExternal(ProjectIndex externalToInternal, String internalPath) {
        for (ProjectInfo project : externalToInternal.getProjects()) {
            String internalBase = project.getPath();
            if (internalBase.endsWith("/")) {
                internalBase = internalBase.substring(0, internalBase.length() - 1);
            }
            if (internalPath.equals(internalBase) || internalPath.startsWith(internalBase + "/")) {
                return baseFolder + getMappedName(project) + internalPath.substring(internalBase.length());
            }
        }

        // Shouldn't occur. If occurred, it's a bug.
        log.warn("Mapping for internal folder '{}' is not found. Use it as is.", internalPath);
        return internalPath;
    }

    public void initialize() throws IOException {
        refreshMappingWithLock();
    }

    /**
     * Load mapping from properties file.
     *
     * @param delegate original repository
     * @param configFile properties file
     * @param baseFolder virtual base folder. WebStudio will think that projects can be found in this folder.
     * @return loaded mapping
     * @throws IOException if it was any error during operation
     */
    private ProjectIndex readExternalToInternalMap(FolderRepository delegate,
            String configFile,
            String baseFolder) throws IOException {
        baseFolder = StringUtils.isBlank(baseFolder) ? "" : baseFolder.endsWith("/") ? baseFolder : baseFolder + "/";
        FileItem fileItem = repositorySettings.getRepository().read(configFile);
        if (fileItem == null) {
            log.debug("Repository configuration file {} is not found.", configFile);
            return generateExternalToInternalMap(delegate, baseFolder);
        }

        if (settingsSyncDate.before(fileItem.getData().getModifiedAt())) {
            settingsSyncDate = fileItem.getData().getModifiedAt();
        }

        TypeDescription projectsDescription = new TypeDescription(ProjectIndex.class);
        projectsDescription.addPropertyParameters("projects", ProjectInfo.class);
        Constructor constructor = new Constructor(ProjectIndex.class);
        constructor.addTypeDescription(projectsDescription);
        Representer representer = new Representer();
        representer.getPropertyUtils().setSkipMissingProperties(true);

        try (InputStream stream = fileItem.getStream();
                InputStreamReader in = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            Yaml yaml = new Yaml(constructor, representer);
            ProjectIndex projectIndex = yaml.loadAs(in, ProjectIndex.class);
            if (projectIndex != null) {
                return projectIndex;
            }
        }

        return new ProjectIndex();
    }

    private boolean syncProjectIndex(FolderRepository delegate, ProjectIndex projectIndex) throws IOException {
        boolean modified = false;
        for (Iterator<ProjectInfo> iterator = projectIndex.getProjects().iterator(); iterator.hasNext();) {
            ProjectInfo project = iterator.next();

            if (delegate.check(project.getPath()) == null) {
                // Folder was removed.
                iterator.remove();
                modified = true;
                log.info("Sync project index: remove project '{}' with path '{}'",
                    project.getName(),
                    project.getPath());
            } else {
                Date modifiedAt = project.getModifiedAt();
                String fullName = project.getPath() + "/rules.xml";
                FileData fileData = delegate.check(fullName);

                if (fileData != null) {
                    if (modifiedAt == null || !modifiedAt.equals(fileData.getModifiedAt())) {
                        // rules.xml was modified. Need to update modification date and project name.
                        project.setModifiedAt(fileData.getModifiedAt());

                        FileItem descriptorItem = delegate.read(fullName);
                        try (InputStream is = descriptorItem.getStream()) {
                            project.setName(getProjectName(is));
                        }
                        log.info("Sync project index: update name to '{}' the project in path '{}'",
                            project.getName(),
                            project.getPath());
                        modified = true;
                    }
                } else {
                    // If we don't have rules.xml, the project name will be folder name.
                    String path = project.getPath();
                    String folderName = path.substring(path.lastIndexOf('/') + 1);
                    if (modifiedAt != null) {
                        // rules.xml was exist before but now it's removed
                        project.setModifiedAt(null);
                        project.setName(folderName);
                        modified = true;
                        log.info("Sync project index: update name to '{}' the project in path '{}'",
                            project.getName(),
                            project.getPath());
                    } else {
                        if (!project.getName().equals(folderName)) {
                            project.setName(folderName);
                            modified = true;
                            log.info("Sync project index: update name to '{}' the project in path {}",
                                project.getName(),
                                project.getPath());
                        }
                    }
                }
            }
        }
        return modified;
    }

    private String createUniquePath(ProjectIndex externalToInternal, String externalPath) {
        // If occasionally such project name exists already, add some suffix to it.
        String projectName = externalPath.substring(baseFolder.length());
        List<ProjectInfo> projects = externalToInternal.getProjects();
        if (projects.stream().anyMatch(p -> projectName.equals(p.getName()))) {
            int i = 1;
            String copy = externalPath + "." + i;
            boolean found = false;
            do {
                for (ProjectInfo p : projects) {
                    if (p.getName().equals(copy.substring(baseFolder.length()))) {
                        found = true;
                        break;
                    }
                }
                copy = externalPath + "." + (++i);
            } while (found);
            externalPath = copy;
        }

        return externalPath;
    }

    /**
     * Detect existing projects and Deploy Configurations based on rules.xml and
     * {@link ArtefactProperties#DESCRIPTORS_FILE}. If there are several projects with same name, suffix will be added
     * to them
     *
     * @param delegate repository to detect projects {@link ArtefactProperties#DESCRIPTORS_FILE}
     * @param baseFolder virtual base folder. WebStudio will think that projects can be found in this folder.
     * @return generated mapping
     */
    private ProjectIndex generateExternalToInternalMap(FolderRepository delegate,
            String baseFolder) throws IOException {
        ProjectIndex externalToInternal = new ProjectIndex();
        List<FileData> allFiles = delegate.list("");
        for (FileData fileData : allFiles) {
            String fullName = fileData.getName();
            String[] nameParts = fullName.split("/");
            if (nameParts.length == 0) {
                continue;
            }
            String fileName = nameParts[nameParts.length - 1];
            if ("rules.xml".equals(fileName)) {
                FileItem fileItem = delegate.read(fullName);
                try (InputStream stream = fileItem.getStream()) {
                    String projectName = getProjectName(stream);
                    String externalPath = createUniquePath(externalToInternal, baseFolder + projectName);

                    int cutSize = "rules.xml".length() + (nameParts.length > 1 ? 1 : 0); // Exclude "/" if exist
                    String path = fullName.substring(0, fullName.length() - cutSize);
                    ProjectInfo project = new ProjectInfo(externalPath.substring(baseFolder.length()), path);
                    project.setModifiedAt(fileItem.getData().getModifiedAt());
                    externalToInternal.getProjects().add(project);
                }
            }
        }
        return externalToInternal;
    }

    private void refreshMappingWithLock() throws IOException {
        repositorySettings.lock(configFile);
        try {
            refreshMapping();
        } finally {
            repositorySettings.unlock(configFile);
        }
    }

    private void refreshMapping() {
        try {
            settingsSyncDate = repositorySettings.getSyncDate();

            this.externalToInternal = readExternalToInternalMap(delegate, configFile, baseFolder);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            this.externalToInternal = new ProjectIndex();
        }
    }

    private ProjectIndex updateConfigFile(FileData folderData) throws IOException {
        FileMappingData mappingData = folderData.getAdditionalData(FileMappingData.class);
        if (mappingData == null) {
            log.warn("Unexpected behavior: FileMappingData is absent.");
            return externalToInternal.copy();
        }

        repositorySettings.lock(configFile);
        try {
            // We must ensure that our externalToInternal.getProjects() is up to date.
            getUpToDateMapping(false);
            ProjectIndex projectIndex = externalToInternal.copy();
            List<ProjectInfo> projects = projectIndex.getProjects();

            Optional<ProjectInfo> project = findProject(projectIndex, folderData);
            String externalPath = mappingData.getExternalPath();
            String projectName = externalPath.startsWith(baseFolder) ? externalPath.substring(baseFolder.length())
                                                                     : externalPath;
            if (project.isPresent()) {
                project.get().setName(projectName);
            } else {
                ProjectInfo info = new ProjectInfo(projectName, mappingData.getInternalPath());
                projects.add(info);
            }

            ByteArrayInputStream configStream = getStreamFromProperties(projectIndex);
            FileData configData = new FileData();
            configData.setName(configFile);
            configData.setAuthor(folderData.getAuthor());
            configData.setComment(folderData.getComment());
            repositorySettings.getRepository().save(configData, configStream);
            return projectIndex;
        } catch (IOException | RuntimeException e) {
            // Failed to update mapping. Restore current saved version.
            refreshMapping();
            throw e;
        } finally {
            repositorySettings.unlock(configFile);
        }
    }

    private ByteArrayInputStream getStreamFromProperties(ProjectIndex projectIndex) throws IOException {
        DumperOptions options = new DumperOptions();
        options.setPrettyFlow(true);
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        Yaml yaml = new Yaml(options);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (Writer writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)) {
            yaml.dump(projectIndex, writer);
        }

        return new ByteArrayInputStream(outputStream.toByteArray());
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

    private boolean isUpdateConfigNeeded(FileData folderData) throws IOException {
        if (supports().branches()) {
            if (!getBranch().equals(getBaseBranch())) {
                // Update project name only on base branch.
                return false;
            }
        }
        FileMappingData mappingData = folderData.getAdditionalData(FileMappingData.class);
        if (mappingData != null) {
            String internalPath = mappingData.getInternalPath();
            String externalPath = baseFolder + getUpToDateMapping(true).getProjects()
                .stream()
                .filter(p -> p.getPath().equals(internalPath))
                .findFirst()
                .map(this::getMappedName)
                .orElse("");
            return !externalPath.equals(mappingData.getExternalPath());
        }
        return false;
    }

    @Override
    public boolean isValidBranchName(String branch) {
        if (delegate.supports().branches()) {
            return ((BranchRepository) delegate).isValidBranchName(branch);
        }
        return true;
    }

    @Override
    public boolean branchExists(String branch) throws IOException {
        return delegate.supports().branches() && ((BranchRepository) delegate).branchExists(branch);
    }

    @Override
    public String getRealPath(String externalPath) {
        ProjectIndex mapping;
        try {
            mapping = getUpToDateMapping(true);
        } catch (IOException e) {
            log.warn(e.getMessage(), e);
            mapping = externalToInternal.copy();
        }
        return toInternal(mapping, externalPath);
    }

    @Override
    public String getBusinessName(String mappedName) {
        int separatorIndex = mappedName.lastIndexOf(SEPARATOR);
        if (separatorIndex >= 0) {
            String projectName = mappedName.substring(0, separatorIndex);
            int subFolderIndex = mappedName.indexOf('/', separatorIndex + 1);
            return subFolderIndex >= 0 ? projectName + mappedName.substring(subFolderIndex) : projectName;
        }
        return mappedName;
    }

    @Override
    public String getMappedName(String businessName, String path) {
        return businessName + MappedRepository.SEPARATOR + getHash(path);
    }

    private String getMappedName(ProjectInfo project) {
        return getMappedName(project.getName(), project.getPath());
    }

    @Override
    public String findMappedName(String internalPath) {
        ProjectIndex mapping;
        try {
            mapping = getUpToDateMapping(true);
        } catch (IOException e) {
            log.warn(e.getMessage(), e);
            mapping = externalToInternal.copy();
        }
        Optional<ProjectInfo> projectInfo = mapping.getProjects()
            .stream()
            .filter(p -> internalPath.equals(p.getPath()) || internalPath.startsWith(p.getPath() + "/"))
            .findFirst();
        return projectInfo.map(p -> {
            String mappedProjectName = baseFolder + getMappedName(p);
            if (internalPath.equals(p.getPath())) {
                return mappedProjectName;
            } else {
                return mappedProjectName + internalPath.substring(p.getPath().length());
            }
        }).orElse(null);
    }

    private String getHash(String s) {
        if (StringUtils.isEmpty(s)) {
            return "";
        }
        if (digest != null) {
            return bytesToHex(digest.digest(s.getBytes(StandardCharsets.UTF_8)));
        } else {
            // Fallback
            return String.valueOf(s.hashCode());
        }
    }

    private static String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
