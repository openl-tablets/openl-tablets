package org.openl.rules.workspace.dtr.impl;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.xml.sax.InputSource;

import org.openl.rules.project.abstraction.ArtefactProperties;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.repository.api.AdditionalData;
import org.openl.rules.repository.api.BranchRepository;
import org.openl.rules.repository.api.BranchStatus;
import org.openl.rules.repository.api.BranchTreeRevision;
import org.openl.rules.repository.api.ChangesetType;
import org.openl.rules.repository.api.ConflictResolveData;
import org.openl.rules.repository.api.Features;
import org.openl.rules.repository.api.FeaturesBuilder;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.FileItem;
import org.openl.rules.repository.api.Listener;
import org.openl.rules.repository.api.Pageable;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.repository.api.SearchableRepository;
import org.openl.rules.repository.api.UserInfo;
import org.openl.rules.workspace.dtr.FolderMapper;
import org.openl.util.FileTypeHelper;
import org.openl.util.FileUtils;
import org.openl.util.HashingUtils;
import org.openl.util.IOUtils;
import org.openl.util.StringUtils;

@Slf4j
public class MappedRepository implements BranchRepository, Closeable, FolderMapper {
    private static final String SEPARATOR = ":";
    private static final int TREE_REVISION_CACHE_CAPACITY = 2_048;
    private static final int DESCRIPTOR_REVISION_CACHE_CAPACITY = 65_536;

    @Getter
    @Setter
    private Repository delegate;

    private final AtomicReference<ProjectIndexCache> indexCache = new AtomicReference<>();
    private final ReadWriteLock indexLock = new ReentrantReadWriteLock();
    private BoundedCache<String, ProjectIndex> indexesByTreeRevision =
            new BoundedCache<>(TREE_REVISION_CACHE_CAPACITY);
    private BoundedCache<String, Optional<String>> projectNamesByDescriptorRevision =
            new BoundedCache<>(DESCRIPTOR_REVISION_CACHE_CAPACITY);

    @Setter(AccessLevel.PRIVATE)
    private String baseFolder;

    public static Repository create(Repository delegate,
                                    String baseFolder) throws IOException {
        MappedRepository mappedRepository = null;
        try {
            mappedRepository = new MappedRepository();
            mappedRepository.setDelegate(delegate);
            mappedRepository.setBaseFolder(baseFolder);
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
    }

    @Override
    public void close() throws IOException {
        indexLock.writeLock().lock();
        try {
            indexCache.set(new ProjectIndexCache(new ProjectIndex()));
        } finally {
            indexLock.writeLock().unlock();
        }

        if (delegate instanceof Closeable closeable) {
            closeable.close();
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
        var mapping = getUpToDateMapping(true);

        var internal = new ArrayList<FileData>();
        for (ProjectInfo project : mapping.getProjects()) {
            var external = baseFolder + getMappedName(project);
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
        var mapping = getUpToDateMapping(true);
        var check = delegate.check(toInternal(mapping, name));
        return toExternal(mapping, check);
    }

    @Override
    public FileItem read(String name) throws IOException {
        var mapping = getUpToDateMapping(true);
        return toExternal(mapping, delegate.read(toInternal(mapping, name)));
    }

    @Override
    public FileData save(FileData data, InputStream stream) throws IOException {
        var mapping = getUpToDateMapping(true);
        return toExternal(mapping, delegate.save(toInternal(mapping, data), stream));
    }

    @Override
    public List<FileData> save(List<FileItem> fileItems) throws IOException {
        var mapping = getUpToDateMapping(true);
        var fileItemsInternal = new ArrayList<FileItem>(fileItems.size());
        for (FileItem fi : fileItems) {
            fileItemsInternal.add(new FileItem(toInternal(mapping, fi.getData()), fi.getStream()));
        }
        var result = delegate.save(fileItemsInternal);

        return toExternal(mapping, result);
    }

    @Override
    public boolean delete(FileData data) throws IOException {
        var mapping = getUpToDateMapping(true);
        var deleted = delegate.delete(toInternal(mapping, data));
        if (deleted) {
            removeMapping(data.getName());
        }
        return deleted;
    }

    @Override
    public boolean delete(List<FileData> data) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setListener(final Listener callback) {
        if (callback == null) {
            // Removing the listener must actually stop the monitor; wrapping null would restart it and trigger a fetch.
            // As a result, application cannot correctly release all resources at shutdown
            delegate.setListener(null);
        } else {
            delegate.setListener(() -> {
                indexLock.writeLock().lock();
                try {
                    var updatedIndex = readExternalToInternalMap(delegate, baseFolder);
                    indexCache.set(new ProjectIndexCache(updatedIndex));
                } catch (Exception e) {
                    log.warn(e.getMessage(), e);
                } finally {
                    indexLock.writeLock().unlock();
                }

                callback.onChange();
            });
        }
    }

    @Override
    public List<FileData> listHistory(String name) throws IOException {
        var mapping = getUpToDateMapping(true);
        return toExternal(mapping, delegate.listHistory(toInternal(mapping, name)));
    }

    @Override
    public List<FileData> listHistory(String name,
                                      String globalFilter,
                                      boolean techRevs,
                                      Pageable pageable) throws IOException {
        var mapping = getUpToDateMapping(true);
        return toExternal(mapping,
                ((SearchableRepository) delegate)
                        .listHistory(toInternal(mapping, name), globalFilter, techRevs, pageable));
    }

    @Override
    public FileData checkHistory(String name, String version) throws IOException {
        var mapping = getUpToDateMapping(true);
        return toExternal(mapping, delegate.checkHistory(toInternal(mapping, name), version));
    }

    @Override
    public FileItem readHistory(String name, String version) throws IOException {
        var mapping = getUpToDateMapping(true);
        return toExternal(mapping, delegate.readHistory(toInternal(mapping, name), version));
    }

    @Override
    public boolean deleteHistory(FileData data) throws IOException {
        if (data.getVersion() != null) {
            return false;
        }
        return delete(data);
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
        var mapping = getUpToDateMapping(true);

        var internal = new ArrayList<FileData>();
        for (ProjectInfo project : mapping.getProjects()) {
            var external = baseFolder + getMappedName(project);
            if (external.startsWith(path) && !external.substring(path.length()).contains("/")) {
                // "external" is direct child of "path"
                var data = delegate.check(project.getPath());
                if (data == null) {
                    // It can be intermediate state: project is added to index, but not still committed.
                    // Or project could be removed from repository, but index is not updated. Will be updated later.
                    log.debug("Project {} is not found.", project.getPath());
                } else {
                    internal.add(data);
                }
            }
        }

        return toExternal(mapping, internal);
    }

    @Override
    public List<FileData> listFiles(String path, String version) throws IOException {
        var mapping = getUpToDateMapping(true);
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
    public Features supports() {
        return new FeaturesBuilder(delegate)
                .setVersions(delegate.supports().versions())
                .setMappedFolders(true)
                .setBranches(delegate.supports().branches())
                .setFolders(delegate.supports().folders())
                .setSupportsUniqueFileId(delegate.supports().uniqueFileId())
                .setSearchable(delegate.supports().searchable())
                .build();
    }

    @Override
    public void merge(String branchFrom, UserInfo author, ConflictResolveData conflictResolveData) throws IOException {
        ((BranchRepository) delegate).merge(branchFrom, author, conflictResolveData);
    }

    @Override
    public String getBaseBranch() {
        return delegate.supports().branches() ? ((BranchRepository) delegate).getBaseBranch() : null;
    }

    @Override
    public Map<String, BranchStatus> getBranchStatuses(Collection<String> branches) throws IOException {
        return ((BranchRepository) delegate).getBranchStatuses(branches);
    }

    @Override
    public Map<String, BranchTreeRevision> getBranchTreeRevisions(Collection<String> branches,
                                                                  String path) throws IOException {
        var internalPath = path;
        if (!path.isEmpty()) {
            internalPath = toInternal(getUpToDateMapping(true), path);
        }
        return ((BranchRepository) delegate).getBranchTreeRevisions(branches, internalPath);
    }

    @Override
    public void pull(UserInfo author) throws IOException {
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
    public void createRepositoryBranch(String branch, @Nullable String startPoint) throws IOException {
        ((BranchRepository) delegate).createRepositoryBranch(branch, startPoint);
    }

    @Override
    public void deleteRepositoryBranch(String branch) throws IOException {
        ((BranchRepository) delegate).deleteRepositoryBranch(branch);
    }

    @Override
    public List<String> listBranches() throws IOException {
        return ((BranchRepository) delegate).listBranches();
    }

    @Override
    public BranchRepository forBranch(String branch) throws IOException {
        var delegateForBranch = ((BranchRepository) delegate).forBranch(branch);

        MappedRepository mappedRepository = null;
        try {
            mappedRepository = new MappedRepository();
            mappedRepository.setDelegate(delegateForBranch);
            mappedRepository.setBaseFolder(baseFolder);
            mappedRepository.indexesByTreeRevision = indexesByTreeRevision;
            mappedRepository.projectNamesByDescriptorRevision = projectNamesByDescriptorRevision;
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
        indexLock.writeLock().lock();
        try {
            if (internal.endsWith("/")) {
                internal = internal.substring(0, internal.length() - 1);
            }

            var fullName = internal + "/rules.xml";
            var fileData = delegate.check(fullName);
            ProjectInfo project;
            if (fileData != null) {
                var descriptorItem = delegate.read(fullName);
                try (var is = descriptorItem.getStream()) {
                    project = new ProjectInfo(getProjectName(is, internal), internal);
                }
            } else {
                project = new ProjectInfo(internal.substring(internal.lastIndexOf('/') + 1), internal);
            }
            var externalToInternal = getUpToDateMapping(false);
            List<ProjectInfo> projectsWithSameName = externalToInternal.getProjects()
                    .stream()
                    .filter(p -> p.getName().equals(project.getName()))
                    .toList();
            if (!projectsWithSameName.isEmpty()) {
                if (projectsWithSameName.stream().anyMatch(p -> p.getPath().equals(project.getPath()))) {
                    throw new IOException("Project \"" + project.getName() + "\" with path \"" + project
                            .getPath() + "\" is already imported.");
                }
            }
            externalToInternal.getProjects().add(project);
            indexCache.set(new ProjectIndexCache(externalToInternal));
        } finally {
            indexLock.writeLock().unlock();
        }
    }

    @Override
    public void removeMapping(String external) throws IOException {
        indexLock.writeLock().lock();
        try {
            var externalToInternal = getUpToDateMapping(false);
            externalToInternal.getProjects()
                    .removeIf(projectInfo -> external.equals(baseFolder + getMappedName(projectInfo)));

            indexCache.set(new ProjectIndexCache(externalToInternal));
        } finally {
            indexLock.writeLock().unlock();
        }
    }

    private Optional<ProjectInfo> findProject(ProjectIndex projectIndex, FileData data) {
        var mappingData = data.getAdditionalData(FileMappingData.class);
        if (mappingData != null) {
            var internalPath = mappingData.getInternalPath();
            return projectIndex.getProjects().stream().filter(p -> internalPath.equals(p.getPath())).findFirst();
        } else {
            String name = data.getName().startsWith(baseFolder) ? data.getName().substring(baseFolder.length())
                    : data.getName();
            return projectIndex.getProjects().stream().filter(p -> name.equals(getMappedName(p))).findFirst();
        }
    }


    /**
     * Get the current in-memory index with refresh check.
     * The index is regenerated from the repository every 30 minutes.
     *
     * @param withLock if true and refresh is needed then WriteLock will be acquired during refreshing. If false, lock
     *                 should be managed outside.
     * @return a copy of the current project index
     */
    private ProjectIndex getUpToDateMapping(boolean withLock) {
        var projectIndex = indexCache.get();
        if (projectIndex == null || projectIndex.isExpired()) {
            if (withLock) {
                indexLock.writeLock().lock();
                try {
                    projectIndex = indexCache.get();
                    if (projectIndex == null || projectIndex.isExpired()) {
                        refreshMapping();
                    }
                } finally {
                    indexLock.writeLock().unlock();
                }
            } else {
                refreshMapping();
            }
        }

        // Use read lock for reading the current index
        if (withLock) {
            indexLock.readLock().lock();
            try {
                return indexCache.get().getCopy();
            } finally {
                indexLock.readLock().unlock();
            }
        } else {
            return indexCache.get().getCopy();
        }
    }

    private Iterable<FileItem> toInternal(final ProjectIndex mapping,
                                          FileData folderData,
                                          final Iterable<FileItem> files) {
        return () -> new Iterator<>() {
            private final Iterator<FileItem> delegate = files.iterator();

            @Override
            public boolean hasNext() {
                return delegate.hasNext();
            }

            @Override
            public FileItem next() {
                var external = delegate.next();
                var data = external.getData();
                String name;
                if (folderData != null && folderData.getAdditionalData(FileMappingData.class) != null) {
                    var path = data.getName();
                    if (path.startsWith(folderData.getName())) {
                        var folderPath = folderData.getAdditionalData(FileMappingData.class).getInternalPath();
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
        var copy = new FileData();
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
            var externalBase = baseFolder + getMappedName(project);
            if (externalPath.equals(externalBase) || externalPath.startsWith(externalBase + "/")) {
                return project.getPath() + externalPath.substring(externalBase.length());
            }
        }

        log.warn("Mapping for external folder '{}' is not found. Use it as is.", externalPath);
        return externalPath;
    }

    private String toInternalPath(ProjectIndex externalToInternal, FileData data) {
        var mappingData = data.getAdditionalData(FileMappingData.class);
        if (mappingData != null) {
            return mappingData.getInternalPath();
        }

        return toInternal(externalToInternal, data.getName());
    }

    private List<FileData> toExternal(ProjectIndex externalToInternal, List<FileData> internal) {
        var external = new ArrayList<FileData>(internal.size());

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

        var name = toExternal(externalToInternal, data.getName());
        data.addAdditionalData(new FileMappingData(name, data.getName()));
        data.setName(name);

        return data;
    }

    private String toExternal(ProjectIndex externalToInternal, String internalPath) {
        for (ProjectInfo project : externalToInternal.getProjects()) {
            var internalBase = project.getPath();
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
        if (indexCache.get() == null) {
            refreshMappingWithLock();
        }
    }

    @Override
    public void validateConnection() throws IOException {
        delegate.validateConnection();
    }

    /**
     * Generate project index by scanning the repository.
     * The index is maintained only in memory and regenerated when needed.
     *
     * @param delegate   original repository
     * @param baseFolder virtual base folder. OpenL Studio will think that projects can be found in this folder.
     * @return generated mapping
     * @throws IOException if it was any error during operation
     */
    private ProjectIndex readExternalToInternalMap(Repository delegate,
                                                   String baseFolder) throws IOException {
        baseFolder = StringUtils.isBlank(baseFolder) ? "" : baseFolder.endsWith("/") ? baseFolder : baseFolder + "/";
        var treeRevisionBefore = getRootTreeRevision(delegate);
        if (treeRevisionBefore != null) {
            var cached = indexesByTreeRevision.get(treeRevisionBefore);
            if (cached != null) {
                return cached.copy();
            }
        }

        var index = generateExternalToInternalMap(delegate, baseFolder);
        var treeRevisionAfter = getRootTreeRevision(delegate);
        if (treeRevisionBefore != null && treeRevisionBefore.equals(treeRevisionAfter)) {
            indexesByTreeRevision.putIfAbsent(treeRevisionBefore, index.copy());
        }
        return index;
    }

    private static @Nullable String getRootTreeRevision(Repository repository) throws IOException {
        if (!(repository instanceof BranchRepository branchRepository) || !repository.supports().branches()) {
            return null;
        }
        var branch = branchRepository.getBranch();
        if (StringUtils.isBlank(branch)) {
            return null;
        }
        var revision = branchRepository.getBranchTreeRevisions(List.of(branch), "").get(branch);
        return revision == null ? null : revision.treeRevision();
    }

    private String createUniquePath(ProjectIndex externalToInternal, String externalPath) {
        // If occasionally such project name exists already, add some suffix to it.
        var projectName = externalPath.substring(baseFolder.length());
        List<ProjectInfo> projects = externalToInternal.getProjects();
        if (projects.stream().anyMatch(p -> projectName.equals(p.getName()))) {
            var i = 1;
            var copy = externalPath + "." + i;
            var found = false;
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
     * @param delegate   repository to detect projects {@link ArtefactProperties#DESCRIPTORS_FILE}
     * @param baseFolder virtual base folder. OpenL Studio will think that projects can be found in this folder.
     * @return generated mapping
     */
    private ProjectIndex generateExternalToInternalMap(Repository delegate, String baseFolder) throws IOException {
        var externalToInternal = new ProjectIndex();
        var folderQueue = new ArrayDeque<FileData>(delegate.listFolders(""));

        while (!folderQueue.isEmpty()) {
            var folderData = folderQueue.poll();
            var folderPath = folderData.getName();

            var projectInfo = tryResolveProjectFromDescriptor(externalToInternal, folderPath, delegate, baseFolder);
            if (projectInfo == null) {
                projectInfo = tryResolveProjectFromExcelFiles(externalToInternal, folderPath, delegate, baseFolder);
            }

            if (projectInfo == null) {
                // No project found, add subfolders for further exploration
                folderQueue.addAll(delegate.listFolders(folderPath + "/"));
            } else {
                externalToInternal.getProjects().add(projectInfo);
            }
        }

        return externalToInternal;
    }

    /**
     * Attempts to resolve a project from a rules.xml descriptor file.
     *
     * @param folderPath the folder path to check
     * @param delegate   the repository
     * @param baseFolder the base folder for external paths
     * @return the resolved ProjectInfo, or null if no project was resolved
     * @throws IOException if an error occurs while reading the descriptor
     */
    private ProjectInfo tryResolveProjectFromDescriptor(ProjectIndex externalToInternal,
                                                        String folderPath,
                                                        Repository delegate,
                                                        String baseFolder) throws IOException {
        var descriptorPath = folderPath + "/rules.xml";
        var rulesDescriptor = delegate.check(descriptorPath);

        if (rulesDescriptor == null) {
            return null;
        }

        var descriptorRevision = rulesDescriptor.getUniqueId();
        var descriptorName = descriptorRevision == null ? null
                : projectNamesByDescriptorRevision.get(descriptorRevision);
        if (descriptorName == null) {
            var fileItem = delegate.read(descriptorPath);
            try (var stream = fileItem.getStream()) {
                // The name is cached even when absent, so a nameless descriptor is not read and parsed again.
                descriptorName = Optional.ofNullable(getProjectName(stream));
            }
            if (descriptorRevision != null) {
                projectNamesByDescriptorRevision.putIfAbsent(descriptorRevision, descriptorName);
            }
        }

        var projectName = ProjectDescriptor.resolveName(descriptorName.orElse(null), FileUtils.getName(folderPath));
        var externalPath = createUniquePath(externalToInternal, baseFolder + projectName);
        return new ProjectInfo(externalPath.substring(baseFolder.length()), folderPath);
    }

    /**
     * Attempts to resolve a project by finding Excel files in the folder.
     * Only looks for Excel files directly in the specified folder, not in subfolders.
     *
     * @param folderPath the folder path to check
     * @param delegate   the repository
     * @param baseFolder the base folder for external paths
     * @return the resolved ProjectInfo, or null if no project was resolved
     * @throws IOException if an error occurs while listing or reading files
     */
    private ProjectInfo tryResolveProjectFromExcelFiles(ProjectIndex externalToInternal,
                                                        String folderPath,
                                                        Repository delegate,
                                                        String baseFolder) throws IOException {
        var allFiles = delegate.list(folderPath + "/");

        for (FileData fileData : allFiles) {
            if (isExcelFileInFolderRoot(fileData, folderPath)) {
                String projectName = FileUtils.getName(folderPath);
                var externalPath = createUniquePath(externalToInternal, baseFolder + projectName);
                return new ProjectInfo(
                        externalPath.substring(baseFolder.length()),
                        folderPath
                );
            }
        }

        return null;
    }

    /**
     * Checks if a file is an Excel file located directly in the specified folder.
     *
     * @param fileData   the file to check
     * @param folderPath the folder path to compare against
     * @return true if the file is an Excel file in the folder root, false otherwise
     */
    private boolean isExcelFileInFolderRoot(FileData fileData, String folderPath) {
        var filePath = fileData.getName();
        // Ensure the file is directly in folderPath, not in a subfolder
        var idx = filePath.lastIndexOf('/');
        if (idx < 0) {
            return false;
        }
        var parentPath = filePath.substring(0, idx);
        if (!Objects.equals(parentPath, folderPath)) {
            return false;
        }
        String fileName = FileUtils.getName(filePath);
        return FileTypeHelper.isExcelFile(fileName);
    }

    private void refreshMappingWithLock() throws IOException {
        indexLock.writeLock().lock();
        try {
            refreshMapping();
        } finally {
            indexLock.writeLock().unlock();
        }
    }

    private void refreshMapping() {
        try {
            var updatedIndex = readExternalToInternalMap(delegate, baseFolder);
            indexCache.set(new ProjectIndexCache(updatedIndex));
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            indexCache.set(new ProjectIndexCache(new ProjectIndex()));
        }
    }

    private ProjectIndex updateConfigFile(FileData folderData) {
        var mappingData = folderData.getAdditionalData(FileMappingData.class);
        if (mappingData == null) {
            log.warn("Unexpected behavior: FileMappingData is absent.");
            return getUpToDateMapping(true);
        }

        indexLock.writeLock().lock();
        try {
            // We must ensure that our externalToInternal.getProjects() is up to date.
            var projectIndex = getUpToDateMapping(false);
            List<ProjectInfo> projects = projectIndex.getProjects();

            var project = findProject(projectIndex, folderData);
            var externalPath = mappingData.getExternalPath();
            String projectName = externalPath.startsWith(baseFolder) ? externalPath.substring(baseFolder.length())
                    : externalPath;
            if (project.isPresent()) {
                project.get().setName(projectName);
            } else {
                var info = new ProjectInfo(projectName, mappingData.getInternalPath());
                projects.add(info);
            }

            // Update in-memory index
            indexCache.set(new ProjectIndexCache(projectIndex));
            return projectIndex;
        } finally {
            indexLock.writeLock().unlock();
        }
    }


    private String getProjectName(InputStream inputStream) {
        try {
            var inputSource = new InputSource(inputStream);
            XPathFactory factory = XPathFactory.newInstance();
            var xPath = factory.newXPath();
            var xPathExpression = xPath.compile("/project/name");
            return xPathExpression.evaluate(inputSource);
        } catch (XPathExpressionException e) {
            return null;
        }
    }

    /**
     * Reads the project name from a rules.xml descriptor, or uses the folder name when the descriptor
     * has no project name. The naming rule is shared with {@link ProjectDescriptor#resolveName}.
     *
     * @param inputStream rules.xml content
     * @param folderPath  project folder path
     * @return the project name, never blank
     */
    private String getProjectName(InputStream inputStream, String folderPath) {
        return ProjectDescriptor.resolveName(getProjectName(inputStream), FileUtils.getName(folderPath));
    }

    private boolean isUpdateConfigNeeded(FileData folderData) throws IOException {
        var mappingData = folderData.getAdditionalData(FileMappingData.class);
        if (mappingData != null) {
            var internalPath = mappingData.getInternalPath();
            var externalPath = baseFolder + getUpToDateMapping(true).getProjects()
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
        var mapping = getUpToDateMapping(true);
        return toInternal(mapping, externalPath);
    }

    @Override
    public String getBusinessName(String mappedName) {
        var separatorIndex = mappedName.lastIndexOf(SEPARATOR);
        if (separatorIndex >= 0) {
            var projectName = mappedName.substring(0, separatorIndex);
            var subFolderIndex = mappedName.indexOf('/', separatorIndex + 1);
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
        var mapping = getUpToDateMapping(true);
        Optional<ProjectInfo> projectInfo = mapping.getProjects()
                .stream()
                .filter(p -> internalPath.equals(p.getPath()) || internalPath.startsWith(p.getPath() + "/"))
                .findFirst();
        return projectInfo.map(p -> {
            var mappedProjectName = baseFolder + getMappedName(p);
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
        return HashingUtils.sha256Hex(s);
    }

    @Override
    public boolean isBranchProtected(String branch) {
        return ((BranchRepository) delegate).isBranchProtected(branch);
    }
}
