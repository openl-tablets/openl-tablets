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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.openl.rules.repository.RRepositoryFactory;
import org.openl.rules.repository.RepositoryMode;
import org.openl.rules.repository.api.*;
import org.openl.rules.repository.exceptions.RRepositoryException;
import org.openl.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.TypeDescription;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.representer.Representer;

public class MappedRepository implements FolderRepository, BranchRepository, RRepositoryFactory, Closeable, FolderMapper {
    private final Logger log = LoggerFactory.getLogger(MappedRepository.class);

    private FolderRepository delegate;

    private volatile ProjectIndex externalToInternal = new ProjectIndex();

    private final ReadWriteLock mappingLock = new ReentrantReadWriteLock();
    private RepositoryMode repositoryMode;
    private String configFile;
    private String baseFolder;
    private RepositorySettings repositorySettings;
    private Date settingsSyncDate = new Date();

    public static Repository create(FolderRepository delegate,
            RepositoryMode repositoryMode,
            String baseFolder,
            RepositorySettings repositorySettings) throws RRepositoryException {
        MappedRepository mappedRepository = new MappedRepository();
        mappedRepository.setDelegate(delegate);
        mappedRepository.setRepositoryMode(repositoryMode);
        mappedRepository.setConfigFile(delegate.getId() + "/openl-projects.yaml");
        mappedRepository.setBaseFolder(baseFolder);
        mappedRepository.setRepositorySettings(repositorySettings);
        mappedRepository.initialize();
        return mappedRepository;
    }

    private MappedRepository() {
    }

    @Override
    public FolderRepository getDelegate() {
        return delegate;
    }

    public void setDelegate(FolderRepository delegate) {
        this.delegate = delegate;
    }

    private void setRepositoryMode(RepositoryMode repositoryMode) {
        this.repositoryMode = repositoryMode;
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

    private void setExternalToInternal(ProjectIndex externalToInternal) {
        this.externalToInternal = externalToInternal;
    }

    @Override
    public void close() throws IOException {
        Lock lock = mappingLock.writeLock();
        try {
            lock.lock();
            externalToInternal = new ProjectIndex();
        } finally {
            lock.unlock();
        }

        if (delegate instanceof Closeable) {
            ((Closeable) delegate).close();
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
        ProjectIndex mapping = getMappingForRead();

        List<FileData> internal = new ArrayList<>();
        for (ProjectInfo project : mapping.getProjects()) {
            String external = baseFolder + project.getName();
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
        ProjectIndex mapping = getMappingForRead();
        FileData check = delegate.check(toInternal(mapping, name));
        if (delegate.supports().versions()) {
            Optional<ProjectInfo> project = externalToInternal.getProjects()
                .stream()
                .filter(p -> name.equals(baseFolder + p.getName()))
                .findFirst();
            if (project.isPresent() && project.get().isArchived()) {
                check.setDeleted(true);
            }
        }
        return toExternal(mapping, check);
    }

    @Override
    public FileItem read(String name) throws IOException {
        ProjectIndex mapping = getMappingForRead();
        return toExternal(mapping, delegate.read(toInternal(mapping, name)));
    }

    @Override
    public FileData save(FileData data, InputStream stream) throws IOException {
        ProjectIndex mapping = getMappingForRead();
        try {
            return toExternal(mapping, delegate.save(toInternal(mapping, data), stream));
        } catch (MergeConflictException e) {
            throw new MergeConflictException(toExternalKeys(mapping, e.getDiffs()),
                e.getBaseCommit(),
                e.getYourCommit(),
                e.getTheirCommit());
        }
    }

    @Override
    public List<FileData> save(List<FileItem> fileItems) throws IOException {
        ProjectIndex mapping = getMappingForRead();
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
            Optional<ProjectInfo> projectInfo = findProject(data);
            if (projectInfo.isPresent()) {
                projectInfo.get().setArchived(true);
                saveProjectIndex(externalToInternal);
                return true;
            }
        }

        ProjectIndex mapping = getMappingForRead();
        return delegate.delete(toInternal(mapping, data));
    }

    @Override
    public void setListener(final Listener callback) {
        delegate.setListener(() -> {
            try {
                initialize();
            } catch (Exception e) {
                log.warn(e.getMessage(), e);
            }

            if (callback != null) {
                callback.onChange();
            }
        });
    }

    @Override
    public List<FileData> listHistory(String name) throws IOException {
        ProjectIndex mapping = getMappingForRead();
        return toExternal(mapping, delegate.listHistory(toInternal(mapping, name)));
    }

    @Override
    public FileData checkHistory(String name, String version) throws IOException {
        ProjectIndex mapping = getMappingForRead();
        return toExternal(mapping, delegate.checkHistory(toInternal(mapping, name), version));
    }

    @Override
    public FileItem readHistory(String name, String version) throws IOException {
        ProjectIndex mapping = getMappingForRead();
        return toExternal(mapping, delegate.readHistory(toInternal(mapping, name), version));
    }

    @Override
    public boolean deleteHistory(FileData data) throws IOException {
        ProjectIndex mapping = getMappingForRead();

        if (data.getVersion() == null) {
            try {
                Lock lock = mappingLock.writeLock();
                try {
                    lock.lock();

                    removeMapping(data.getName());
                } finally {
                    lock.unlock();
                }

                // Use mapping before modification
                return delegate.deleteHistory(toInternal(mapping, data));
            } catch (IOException | RuntimeException e) {
                refreshMappingWithLock();
                throw e;
            }
        } else {
            if (delegate.supports().versions()) {
                Optional<ProjectInfo> project = findProject(data);
                if (project.isPresent()) {
                    ProjectInfo projectInfo = project.get();
                    if (projectInfo.isArchived()) {
                        projectInfo.setArchived(false);
                        saveProjectIndex(externalToInternal);
                        return true;
                    }
                }
            }
            return delegate.deleteHistory(toInternal(mapping, data));
        }
    }

    @Override
    public FileData copyHistory(String srcName, FileData destData, String version) throws IOException {
        if (isUpdateConfigNeeded(destData)) {
            try {
                ByteArrayInputStream configStream = updateConfigFile(destData);
                FileData configData = new FileData();
                configData.setName(configFile);
                configData.setAuthor(destData.getAuthor());
                configData.setComment(destData.getComment());
                repositorySettings.getRepository().save(configData, configStream);

                ProjectIndex mapping = getMappingForRead();
                return toExternal(mapping,
                    delegate.copyHistory(toInternal(mapping, srcName), toInternal(mapping, destData), version));
            } catch (IOException | RuntimeException e) {
                // Failed to update mapping. Restore current saved version.
                refreshMappingWithLock();
                throw e;
            }
        } else {
            ProjectIndex mapping = getMappingForRead();
            return toExternal(mapping,
                delegate.copyHistory(toInternal(mapping, srcName), toInternal(mapping, destData), version));
        }
    }

    @Override
    public List<FileData> listFolders(String path) throws IOException {
        ProjectIndex mapping = getMappingForRead();

        List<FileData> internal = new ArrayList<>();
        for (ProjectInfo project : mapping.getProjects()) {
            String external = baseFolder + project.getName();
            if (external.startsWith(path) && !external.substring(path.length()).contains("/")) {
                // "external" is direct child of "path"
                FileData data = delegate.check(project.getPath());
                if (data == null) {
                    log.error("Project {} is not found.", project.getPath());
                } else {
                    if (delegate.supports().versions()) {
                        if (project.isArchived()) {
                            data.setDeleted(true);
                        }
                    }
                    internal.add(data);
                }
            }
        }

        return toExternal(mapping, internal);
    }

    @Override
    public List<FileData> listFiles(String path, String version) throws IOException {
        ProjectIndex mapping = getMappingForRead();
        return toExternal(mapping, delegate.listFiles(toInternal(mapping, path), version));
    }

    @Override
    public FileData save(FileData folderData,
            Iterable<FileItem> files,
            ChangesetType changesetType) throws IOException {
        if (isUpdateConfigNeeded(folderData)) {
            try {
                ByteArrayInputStream configStream = updateConfigFile(folderData);
                FileData configData = new FileData();
                configData.setName(configFile);
                configData.setAuthor(folderData.getAuthor());
                configData.setComment(folderData.getComment());
                repositorySettings.getRepository().save(configData, configStream);
            } catch (IOException | RuntimeException e) {
                // Failed to update mapping. Restore current saved version.
                refreshMappingWithLock();
                throw e;
            }
        }
        try {
            ProjectIndex mapping = getMappingForRead();
            return toExternal(mapping,
                delegate.save(toInternal(mapping, folderData), toInternal(mapping, files), changesetType));
        } catch (MergeConflictException e) {
            ProjectIndex mapping = getMappingForRead();
            throw new MergeConflictException(toExternalKeys(mapping, e.getDiffs()),
                e.getBaseCommit(),
                e.getYourCommit(),
                e.getTheirCommit());

        }
    }

    @Override
    public List<FileData> save(List<FolderItem> folderItems, ChangesetType changesetType) throws IOException {
        if (folderItems.isEmpty()) {
            return Collections.emptyList();
        }

        if (folderItems.get(0).getData().getAdditionalData(FileMappingData.class) != null) {
            throw new UnsupportedOperationException("File name mapping is not supported.");
        }
        ProjectIndex mapping = getMappingForRead();

        List<FolderItem> folderItemsInternal = new ArrayList<>(folderItems.size());
        for (FolderItem fi : folderItems) {
            folderItemsInternal
                .add(new FolderItem(toInternal(mapping, fi.getData()), toInternal(mapping, fi.getFiles())));
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
    public void createBranch(String projectName, String branch) throws IOException {
        ((BranchRepository) delegate).createBranch(projectName, branch);
    }

    @Override
    public void deleteBranch(String projectName, String branch) throws IOException {
        ((BranchRepository) delegate).deleteBranch(projectName, branch);
    }

    @Override
    public List<String> getBranches(String projectName) throws IOException {
        return ((BranchRepository) delegate).getBranches(projectName);
    }

    @Override
    public BranchRepository forBranch(String branch) throws IOException {
        BranchRepository delegateForBranch = ((BranchRepository) delegate).forBranch(branch);

        MappedRepository mappedRepository = new MappedRepository();
        mappedRepository.setDelegate((FolderRepository) delegateForBranch);
        mappedRepository.setRepositoryMode(repositoryMode);
        mappedRepository.setConfigFile(configFile);
        mappedRepository.setBaseFolder(baseFolder);
        mappedRepository.setRepositorySettings(repositorySettings);
        try {
            mappedRepository.initialize();
        } catch (RRepositoryException e) {
            throw new IOException(e.getMessage(), e);
        }

        return mappedRepository;
    }

    @Override
    public void addMapping(String internal) throws IOException {
        Lock lock = mappingLock.writeLock();
        try {
            lock.lock();
            if (internal.endsWith("/")) {
                internal = internal.substring(0, internal.length() - 1);
            }
            ProjectInfo project = new ProjectInfo();
            project.setPath(internal);

            String fullName = internal + "/rules.xml";
            FileData fileData = delegate.check(fullName);
            if (fileData != null) {
                FileItem descriptorItem = delegate.read(fullName);
                try (InputStream is = descriptorItem.getStream()) {
                    project.setName(getProjectName(is));
                }
            } else {
                project.setName(internal.substring(internal.lastIndexOf('/') + 1));
            }

            ProjectIndex externalToInternal = getUpToDateMapping();
            externalToInternal.getProjects().add(project);

            saveProjectIndex(externalToInternal);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void renameMapping(String externalBefore, String externalAfter) throws IOException {
        Lock lock = mappingLock.writeLock();
        try {
            lock.lock();
            ProjectIndex externalToInternal = getUpToDateMapping();
            externalToInternal.getProjects()
                .stream()
                .filter(p -> externalBefore.equals(baseFolder + p.getName()))
                .findFirst().ifPresent(p -> p.setName(externalAfter.substring(baseFolder.length())));

            saveProjectIndex(externalToInternal);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void removeMapping(String external) throws IOException {
        Lock lock = mappingLock.writeLock();
        try {
            lock.lock();
            ProjectIndex externalToInternal = getUpToDateMapping();
            externalToInternal.getProjects()
                .removeIf(projectInfo -> external.equals(baseFolder + projectInfo.getName()));

            saveProjectIndex(externalToInternal);
        } finally {
            lock.unlock();
        }
    }

    private Optional<ProjectInfo> findProject(FileData data) {
        String name = data.getName().startsWith(baseFolder) ?
                      data.getName().substring(baseFolder.length()) :
                      data.getName();
        return getUpToDateMapping().getProjects()
            .stream()
            .filter(p -> name.equals(p.getName()))
            .findFirst();
    }

    private void saveProjectIndex(ProjectIndex projectIndex) throws IOException {
        ByteArrayInputStream configInputStream = getStreamFromProperties(projectIndex);

        FileData configData = new FileData();
        configData.setName(configFile);
        configData.setAuthor(getClass().getName());
        configData.setComment("Update mapping");
        repositorySettings.getRepository().save(configData, configInputStream);
    }

    private ProjectIndex getMappingForRead() {
        Lock lock = mappingLock.readLock();
        ProjectIndex mapping;
        try {
            lock.lock();
            mapping = getUpToDateMapping();
        } finally {
            lock.unlock();
        }
        return mapping;
    }

    private ProjectIndex getUpToDateMapping() {
        if (!repositorySettings.getSyncDate().equals(settingsSyncDate)) {
            refreshMapping();
        }

        return externalToInternal;
    }

    private Iterable<FileItem> toInternal(final ProjectIndex mapping, final Iterable<FileItem> files) {
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
                String name = toInternal(mapping, external.getData().getName());
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
        copy.setName(toInternal(externalToInternal, data.getName()));

        for (AdditionalData<?> value : data.getAdditionalData().values()) {
            copy.addAdditionalData(value.convertPaths(oldPath -> toInternal(externalToInternal, oldPath)));
        }

        return copy;
    }

    private String toInternal(ProjectIndex externalToInternal, String externalPath) {
        for (ProjectInfo project : externalToInternal.getProjects()) {
            String externalBase = baseFolder + project.getName();
            if (externalPath.equals(externalBase) || externalPath.startsWith(externalBase + "/")) {
                return project.getPath() + externalPath.substring(externalBase.length());
            }
        }

        log.debug("Mapping for external folder '{}' is not found. Use it as is.", externalPath);
        return externalPath;
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

        data.setName(toExternal(externalToInternal, data.getName()));
        return data;
    }

    private Map<String, String> toExternalKeys(ProjectIndex externalToInternal, Map<String, String> internal) {
        Map<String, String> external = new LinkedHashMap<>(internal.size());

        for (Map.Entry<String, String> entry : internal.entrySet()) {
            external.put(toExternal(externalToInternal, entry.getKey()), entry.getValue());
        }

        return external;
    }

    private String toExternal(ProjectIndex externalToInternal, String internalPath) {
        for (ProjectInfo project : externalToInternal.getProjects()) {
            String internalBase = project.getPath();
            if (internalBase.endsWith("/")) {
                internalBase = internalBase.substring(0, internalBase.length() - 1);
            }
            if (internalPath.equals(internalBase) || internalPath.startsWith(internalBase + "/")) {
                return baseFolder + project.getName() + internalPath.substring(internalBase.length());
            }
        }

        // Shouldn't occur. If occurred, it's a bug.
        log.warn("Mapping for internal folder '{}' is not found. Use it as is.", internalPath);
        return internalPath;
    }

    @Override
    public void initialize() throws RRepositoryException {
        try {
            refreshMapping();
        } catch (Exception e) {
            throw new RRepositoryException(e.getMessage(), e);
        }
    }

    /**
     * Load mapping from properties file.
     *
     * @param delegate original repository
     * @param repositoryMode Repository mode: design or deploy config.
     * @param configFile properties file
     * @param baseFolder virtual base folder. WebStudio will think that projects can be found in this folder.
     * @return loaded mapping
     * @throws IOException if it was any error during operation
     */
    private ProjectIndex readExternalToInternalMap(FolderRepository delegate,
            RepositoryMode repositoryMode,
            String configFile,
            String baseFolder) throws IOException {
        baseFolder = StringUtils.isBlank(baseFolder) ? "" : baseFolder.endsWith("/") ? baseFolder : baseFolder + "/";
        FileItem fileItem = repositorySettings.getRepository().read(configFile);
        if (fileItem == null) {
            log.debug("Repository configuration file {} is not found.", configFile);
            return generateExternalToInternalMap(delegate, repositoryMode, baseFolder);
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
                boolean modified = syncProjectIndex(delegate, repositoryMode, projectIndex);
                if (modified) {
                    saveProjectIndex(projectIndex);
                }

                return projectIndex;
            }
        }

        return new ProjectIndex();
    }

    private boolean syncProjectIndex(FolderRepository delegate,
        RepositoryMode repositoryMode,
        ProjectIndex projectIndex) throws IOException {
        boolean modified = false;
        if (repositoryMode == RepositoryMode.DESIGN) {
            for (Iterator<ProjectInfo> iterator = projectIndex.getProjects().iterator(); iterator.hasNext(); ) {
                ProjectInfo project = iterator.next();

                if (delegate.check(project.getPath()) == null) {
                    // Folder was removed.
                    iterator.remove();
                    modified = true;
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
                        } else {
                            if (!project.getName().equals(folderName)) {
                                project.setName(folderName);
                                modified = true;
                            }
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
     * @param delegate repository to detect projects
     * @param repositoryMode repository mode. If design repository, rules.xml will be searched, otherwise
     *            {@link ArtefactProperties#DESCRIPTORS_FILE}
     * @param baseFolder virtual base folder. WebStudio will think that projects can be found in this folder.
     * @return generated mapping
     */
    private ProjectIndex generateExternalToInternalMap(FolderRepository delegate,
            RepositoryMode repositoryMode,
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
            if (repositoryMode == RepositoryMode.DESIGN) {
                if ("rules.xml".equals(fileName)) {
                    FileItem fileItem = delegate.read(fullName);
                    try (InputStream stream = fileItem.getStream()) {
                        String projectName = getProjectName(stream);
                        String externalPath = createUniquePath(externalToInternal, baseFolder + projectName);

                        int cutSize = "rules.xml".length() + (nameParts.length > 1 ? 1 : 0); // Exclude "/" if exist
                        String path = fullName.substring(0, fullName.length() - cutSize);
                        ProjectInfo project = new ProjectInfo();
                        project.setName(externalPath.substring(baseFolder.length()));
                        project.setPath(path);
                        project.setModifiedAt(fileItem.getData().getModifiedAt());
                        externalToInternal.getProjects().add(project);
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
                    ProjectInfo project = new ProjectInfo();
                    project.setName(externalPath.substring(baseFolder.length()));
                    project.setPath(path);
                    externalToInternal.getProjects().add(project);
                }
            }
        }

        return externalToInternal;
    }

    private void refreshMappingWithLock() {
        Lock lock = mappingLock.writeLock();
        try {
            lock.lock();
            refreshMapping();
        } finally {
            lock.unlock();
        }
    }

    private void refreshMapping() {
        try {
            settingsSyncDate = repositorySettings.getSyncDate();
            ProjectIndex currentMapping = readExternalToInternalMap(delegate,
                repositoryMode,
                configFile,
                baseFolder);

            setExternalToInternal(currentMapping);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            setExternalToInternal(new ProjectIndex());
        }
    }

    private ByteArrayInputStream updateConfigFile(FileData folderData) throws IOException {
        FileMappingData mappingData = folderData.getAdditionalData(FileMappingData.class);
        if (mappingData == null) {
            log.warn("Unexpected behavior: FileMappingData is absent.");
            return null;
        }

        Lock lock = mappingLock.writeLock();
        try {
            lock.lock();
            List<ProjectInfo> projects = externalToInternal.getProjects();
            Optional<ProjectInfo> project = findProject(folderData);
            if (project.isPresent()) {
                project.get().setPath(mappingData.getInternalPath());
            } else {
                String projectName = folderData.getName().substring(baseFolder.length());
                ProjectInfo info = new ProjectInfo();
                info.setName(projectName);
                info.setPath(mappingData.getInternalPath());
                projects.add(info);
            }

            return getStreamFromProperties(externalToInternal);
        } finally {
            lock.unlock();
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

    private boolean isUpdateConfigNeeded(FileData folderData) {
        FileMappingData mappingData = folderData.getAdditionalData(FileMappingData.class);
        if (mappingData != null) {
            String external = folderData.getName();
            String internal = getMappingForRead().getProjects()
                .stream()
                .filter(p -> (baseFolder + p.getName()).equals(external))
                .findFirst().map(ProjectInfo::getPath).orElse(null);
            return !mappingData.getInternalPath().equals(internal);
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
        ProjectIndex mapping = getMappingForRead();
        return toInternal(mapping, externalPath);
    }
}
