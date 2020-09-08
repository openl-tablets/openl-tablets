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
        externalToInternal = new ProjectIndex();

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
        ProjectIndex mapping = getUpToDateMapping(true);

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
        ProjectIndex mapping = getUpToDateMapping(true);
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
        ProjectIndex mapping = getUpToDateMapping(true);
        return toExternal(mapping, delegate.read(toInternal(mapping, name)));
    }

    @Override
    public FileData save(FileData data, InputStream stream) throws IOException {
        ProjectIndex mapping = getUpToDateMapping(true);
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
                Optional<ProjectInfo> projectInfo = findProject(data);
                if (projectInfo.isPresent()) {
                    projectInfo.get().setArchived(true);
                    saveProjectIndex(externalToInternal);
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
            repositorySettings.lock(configFile);
            try {
                refreshMapping();

                ProjectIndex projectIndex = externalToInternal;
                boolean modified = syncProjectIndex(delegate, repositoryMode, projectIndex);
                if (modified) {
                    saveProjectIndex(projectIndex);
                    setExternalToInternal(projectIndex);
                }
            } catch (Exception e) {
                log.warn(e.getMessage(), e);
            } finally {
                repositorySettings.unlock(configFile);
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
                    Optional<ProjectInfo> project = findProject(data);
                    if (project.isPresent()) {
                        ProjectInfo projectInfo = project.get();
                        if (projectInfo.isArchived()) {
                            projectInfo.setArchived(false);
                            saveProjectIndex(externalToInternal);
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
        try {
            return toExternal(mapping,
                delegate.save(toInternal(mapping, folderData), toInternal(mapping, files), changesetType));
        } catch (MergeConflictException e) {
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
        ProjectIndex mapping = getUpToDateMapping(true);

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
        repositorySettings.lock(configFile);
        try {
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

            ProjectIndex externalToInternal = getUpToDateMapping(false);
            externalToInternal.getProjects().add(project);

            saveProjectIndex(externalToInternal);
        } finally {
            repositorySettings.unlock(configFile);
        }
    }

    @Override
    public void renameMapping(String externalBefore, String externalAfter) throws IOException {
        repositorySettings.lock(configFile);
        try {
            ProjectIndex externalToInternal = getUpToDateMapping(false);
            externalToInternal.getProjects()
                .stream()
                .filter(p -> externalBefore.equals(baseFolder + p.getName()))
                .findFirst().ifPresent(p -> p.setName(externalAfter.substring(baseFolder.length())));

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
                .removeIf(projectInfo -> external.equals(baseFolder + projectInfo.getName()));

            saveProjectIndex(externalToInternal);
        } finally {
            repositorySettings.unlock(configFile);
        }
    }

    private Optional<ProjectInfo> findProject(FileData data) {
        String name = data.getName().startsWith(baseFolder) ?
                      data.getName().substring(baseFolder.length()) :
                      data.getName();
        return getUpToDateMapping(false).getProjects()
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

    /**
     * Check if mapping should be refreshed and if should, read it from file.
     * 
     * @param withLock if true and refresh is needed then lock file will be created during reading. If false, lock
     *            should be managed outside. If refresh isn't needed, lock file will not be created, this flag doesn't
     *            matter.
     */
    private ProjectIndex getUpToDateMapping(boolean withLock) {
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

        data.addAdditionalData(new FileMappingData(data.getName()));
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
            refreshMappingWithLock();
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

    private ProjectIndex updateConfigFile(FileData folderData) throws IOException {
        FileMappingData mappingData = folderData.getAdditionalData(FileMappingData.class);
        if (mappingData == null) {
            log.warn("Unexpected behavior: FileMappingData is absent.");
            return externalToInternal;
        }

        repositorySettings.lock(configFile);
        try {
            // We must ensure that our externalToInternal.getProjects() is up to date.
            getUpToDateMapping(false);
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

            ByteArrayInputStream configStream = getStreamFromProperties(externalToInternal);
            FileData configData = new FileData();
            configData.setName(configFile);
            configData.setAuthor(folderData.getAuthor());
            configData.setComment(folderData.getComment());
            repositorySettings.getRepository().save(configData, configStream);
            return externalToInternal;
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

    private boolean isUpdateConfigNeeded(FileData folderData) {
        FileMappingData mappingData = folderData.getAdditionalData(FileMappingData.class);
        if (mappingData != null) {
            String external = folderData.getName();
            String internal = getUpToDateMapping(true).getProjects()
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
        ProjectIndex mapping = getUpToDateMapping(true);
        return toInternal(mapping, externalPath);
    }
}
