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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Predicate;
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
    private static final int FOLDER_HASH_CACHE_CAPACITY = 4_096;

    @Getter
    @Setter
    private Repository delegate;

    private final AtomicReference<ProjectIndexCache> indexCache = new AtomicReference<>();
    /*
     * The locks are taken in this order: rebuildLock, then the locks of the delegate, then indexLock. indexLock is
     * never held while the delegate is called: a save holds the lock of the delegate while it checks its files
     * against the index, so a thread holding indexLock while it waits for the delegate would deadlock with it.
     */
    private final ReadWriteLock indexLock = new ReentrantReadWriteLock();
    /** Lets one thread at a time rebuild the index, so a rebuild never publishes an index older than the last one. */
    private final Lock rebuildLock = new ReentrantLock();
    /** The folders being saved under a new mapping. A rebuild keeps their mapping, which its scan may not find yet. */
    private final Set<FileData> foldersBeingMapped = ConcurrentHashMap.newKeySet();
    private BoundedCache<String, ProjectIndex> indexesByTreeRevision =
            new BoundedCache<>(TREE_REVISION_CACHE_CAPACITY);
    private BoundedCache<String, String> projectNamesByDescriptorRevision =
            new BoundedCache<>(DESCRIPTOR_REVISION_CACHE_CAPACITY);
    private Map<String, String> hashesByPath = new ConcurrentHashMap<>();

    @Setter(AccessLevel.PRIVATE)
    private String baseFolder;

    @Setter(AccessLevel.PRIVATE)
    private boolean includeExcelFilesInProjectDiscovery;

    public static Repository create(Repository delegate,
                                    String baseFolder) throws IOException {
        return create(delegate, baseFolder, false);
    }

    static Repository create(Repository delegate,
                             String baseFolder,
                             boolean includeExcelFilesInProjectDiscovery) throws IOException {
        MappedRepository mappedRepository = null;
        try {
            mappedRepository = new MappedRepository();
            mappedRepository.setDelegate(delegate);
            mappedRepository.setBaseFolder(baseFolder);
            mappedRepository.setIncludeExcelFilesInProjectDiscovery(includeExcelFilesInProjectDiscovery);
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
        var mapping = getUpToDateMapping();

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
        var mapping = getUpToDateMapping();
        var check = delegate.check(toInternal(mapping, name));
        return toExternal(mapping, check);
    }

    @Override
    public FileItem read(String name) throws IOException {
        var mapping = getUpToDateMapping();
        return toExternal(mapping, delegate.read(toInternal(mapping, name)));
    }

    @Override
    public FileData save(FileData data, InputStream stream) throws IOException {
        var mapping = getUpToDateMapping();
        return toExternal(mapping, delegate.save(toInternal(mapping, data), stream));
    }

    @Override
    public List<FileData> save(List<FileItem> fileItems) throws IOException {
        var mapping = getUpToDateMapping();
        var fileItemsInternal = new ArrayList<FileItem>(fileItems.size());
        for (FileItem fi : fileItems) {
            fileItemsInternal.add(new FileItem(toInternal(mapping, fi.getData()), fi.getStream()));
        }
        var result = delegate.save(fileItemsInternal);

        return toExternal(mapping, result);
    }

    @Override
    public boolean delete(FileData data) throws IOException {
        var mapping = getUpToDateMapping();
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
                try {
                    rebuildIndex();
                } catch (Exception e) {
                    log.warn(e.getMessage(), e);
                }

                callback.onChange();
            });
        }
    }

    @Override
    public List<FileData> listHistory(String name) throws IOException {
        var mapping = getUpToDateMapping();
        return toExternal(mapping, delegate.listHistory(toInternal(mapping, name)));
    }

    @Override
    public List<FileData> listHistory(String name,
                                      String globalFilter,
                                      boolean techRevs,
                                      Pageable pageable) throws IOException {
        var mapping = getUpToDateMapping();
        return toExternal(mapping,
                ((SearchableRepository) delegate)
                        .listHistory(toInternal(mapping, name), globalFilter, techRevs, pageable));
    }

    @Override
    public FileData checkHistory(String name, String version) throws IOException {
        var mapping = getUpToDateMapping();
        return toExternal(mapping, delegate.checkHistory(toInternal(mapping, name), version));
    }

    @Override
    public FileItem readHistory(String name, String version) throws IOException {
        var mapping = getUpToDateMapping();
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
        return withFolderMapping(destData, mapping -> toExternal(mapping,
                delegate.copyHistory(toInternal(mapping, srcName), toInternal(mapping, destData), version)));
    }

    @Override
    public List<FileData> listFolders(String path) throws IOException {
        var mapping = getUpToDateMapping();

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
        var mapping = getUpToDateMapping();
        return toExternal(mapping, delegate.listFiles(toInternal(mapping, path), version));
    }

    @Override
    public FileData save(FileData folderData,
                         Iterable<FileItem> files,
                         ChangesetType changesetType) throws IOException {
        return withFolderMapping(folderData, mapping -> toExternal(mapping,
                delegate.save(toInternal(mapping, folderData), toInternal(mapping, folderData, files), changesetType)));
    }

    /**
     * Calls the delegate with the mapping of the folder the data is written to.
     *
     * <p>A folder written under a new mapping is mapped before the call. A rebuild keeps that mapping until the call
     * ends: its scan may not find the folder until the delegate has written it.
     */
    private <T> T withFolderMapping(FileData folderData, MappedCall<T> call) throws IOException {
        if (!isUpdateConfigNeeded(folderData)) {
            return call.apply(getUpToDateMapping());
        }
        foldersBeingMapped.add(folderData);
        try {
            return call.apply(updateConfigFile(folderData));
        } finally {
            stopKeepingMapping(folderData);
        }
    }

    /**
     * Lets a rebuild drop the mapping of the folder, which the delegate has written or failed to write. A rebuild
     * that scanned the repository before the write scans it again.
     */
    private void stopKeepingMapping(FileData folderData) {
        indexLock.writeLock().lock();
        try {
            foldersBeingMapped.remove(folderData);
            var cache = indexCache.get();
            indexCache.set(new ProjectIndexCache(cache.index(), cache.lastUpdateTime()));
        } finally {
            indexLock.writeLock().unlock();
        }
    }

    /** A call of the delegate, whose paths are translated with the given mapping. */
    @FunctionalInterface
    private interface MappedCall<T> {
        T apply(ProjectIndex mapping) throws IOException;
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
            internalPath = toInternal(getUpToDateMapping(), path);
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

        var mappedRepository = new MappedRepository();
        mappedRepository.setDelegate(delegateForBranch);
        mappedRepository.setBaseFolder(baseFolder);
        mappedRepository.setIncludeExcelFilesInProjectDiscovery(includeExcelFilesInProjectDiscovery);
        // Share the caches so a lazy refresh can reuse a mapping already built for the same tree.
        mappedRepository.indexesByTreeRevision = indexesByTreeRevision;
        mappedRepository.projectNamesByDescriptorRevision = projectNamesByDescriptorRevision;
        mappedRepository.hashesByPath = hashesByPath;
        // The project mapping is built lazily on first access (see getUpToDateMapping), so selecting a branch
        // stays cheap. Eagerly scanning every branch on startup froze repositories that hold many branches.
        return mappedRepository;
    }

    @Override
    public void addMapping(String internal) throws IOException {
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
        refreshExpiredMapping();
        indexLock.writeLock().lock();
        try {
            var externalToInternal = currentMappingCopy();
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
        refreshExpiredMapping();
        indexLock.writeLock().lock();
        try {
            var externalToInternal = currentMappingCopy();
            Predicate<ProjectInfo> mapped = projectInfo -> external.equals(baseFolder + getMappedName(projectInfo));
            var projects = externalToInternal.getProjects();
            var discardedFolders = projects.stream().filter(mapped).map(ProjectInfo::getPath).toList();
            projects.removeIf(mapped);
            // Last, because naming the projects that stay is what tells them apart, and that names this one too.
            discardedFolders.forEach(hashesByPath::remove);

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
     * @return a copy of the current project index
     */
    private ProjectIndex getUpToDateMapping() {
        refreshExpiredMapping();
        // Use read lock for reading the current index
        indexLock.readLock().lock();
        try {
            return currentMappingCopy();
        } finally {
            indexLock.readLock().unlock();
        }
    }

    /** Returns a copy of the index in use. The caller has refreshed the index and holds {@link #indexLock}. */
    private ProjectIndex currentMappingCopy() {
        return indexCache.get().getCopy();
    }

    /**
     * Rebuilds the mapping when there is none yet or when it has expired.
     *
     * <p>The first mapping is waited for. An expired one is rebuilt only when no other thread is rebuilding it, and is
     * served meanwhile: the caller may hold a lock of the delegate that the other rebuild waits for.
     */
    private void refreshExpiredMapping() {
        var cache = indexCache.get();
        if (cache == null) {
            buildFirstMapping();
        } else if (cache.isExpired() && rebuildLock.tryLock()) {
            try {
                // Another thread may have rebuilt it since
                if (indexCache.get().isExpired()) {
                    refreshMapping();
                }
            } finally {
                rebuildLock.unlock();
            }
        }
    }

    /** Builds the first mapping, unless another thread has built it meanwhile. */
    private void buildFirstMapping() {
        rebuildLock.lock();
        try {
            if (indexCache.get() == null) {
                refreshMapping();
            }
        } finally {
            rebuildLock.unlock();
        }
    }

    /** The mapping in use, shared with the caller, which must not change it. */
    private ProjectIndex currentMapping() {
        refreshExpiredMapping();
        return indexCache.get().index();
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

    public void initialize() {
        buildFirstMapping();
    }

    @Override
    public void validateConnection() throws IOException {
        delegate.validateConnection();
    }

    /**
     * Generate project index by scanning the repository.
     * The index is maintained only in memory and regenerated when needed.
     *
     * @param delegate original repository
     * @return generated mapping
     * @throws IOException if it was any error during operation
     */
    private ProjectIndex readExternalToInternalMap(Repository delegate) throws IOException {
        var treeRevisionBefore = getRootTreeRevision(delegate);
        if (treeRevisionBefore != null) {
            var cached = indexesByTreeRevision.get(treeRevisionBefore);
            if (cached != null) {
                return cached.copy();
            }
        }

        var index = generateExternalToInternalMap(delegate);
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

    /**
     * Detect existing projects and Deploy Configurations based on rules.xml and
     * {@link ArtefactProperties#DESCRIPTORS_FILE}. When Excel file discovery is enabled, a folder without a descriptor
     * is also a project if it contains an Excel file in its root.
     *
     * <p>Every project keeps the name its descriptor declares. Several folders may declare the same name; they stay
     * separate projects, told apart by the folder they live in.
     *
     * @param delegate repository to detect projects {@link ArtefactProperties#DESCRIPTORS_FILE}
     * @return generated mapping
     */
    private ProjectIndex generateExternalToInternalMap(Repository delegate) throws IOException {
        var externalToInternal = new ProjectIndex();
        var folderQueue = new ArrayDeque<>(delegate.listFolders(""));

        while (!folderQueue.isEmpty()) {
            var folderData = folderQueue.poll();
            var folderPath = folderData.getName();

            var projectInfo = tryResolveProjectFromDescriptor(folderPath, delegate);
            if (projectInfo == null && includeExcelFilesInProjectDiscovery) {
                projectInfo = tryResolveProjectFromExcelFiles(folderPath, delegate);
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
     * @return the resolved ProjectInfo, or null if no project was resolved
     * @throws IOException if an error occurs while reading the descriptor
     */
    private ProjectInfo tryResolveProjectFromDescriptor(String folderPath, Repository delegate) throws IOException {
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
                // A nameless descriptor is cached as an empty name, so it is not read and parsed again.
                descriptorName = Objects.requireNonNullElse(getProjectName(stream), "");
            }
            if (descriptorRevision != null) {
                projectNamesByDescriptorRevision.putIfAbsent(descriptorRevision, descriptorName);
            }
        }

        var projectName = ProjectDescriptor.resolveName(descriptorName, FileUtils.getName(folderPath));
        return new ProjectInfo(projectName, folderPath);
    }

    /**
     * Attempts to resolve a project by finding Excel files in the folder.
     * Only looks for Excel files directly in the specified folder, not in subfolders.
     *
     * @param folderPath the folder path to check
     * @param delegate   the repository
     * @return the resolved ProjectInfo, or null if no project was resolved
     * @throws IOException if an error occurs while listing or reading files
     */
    private ProjectInfo tryResolveProjectFromExcelFiles(String folderPath, Repository delegate) throws IOException {
        var allFiles = delegate.list(folderPath + "/");

        for (FileData fileData : allFiles) {
            if (isExcelFileInFolderRoot(fileData, folderPath)) {
                return new ProjectInfo(FileUtils.getName(folderPath), folderPath);
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

    /** Rebuilds the mapping, or leaves an empty one when the repository cannot be read. */
    private void refreshMapping() {
        rebuildLock.lock();
        var scannedOver = indexCache.get();
        try {
            rebuildIndex();
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            // A mapping changed during the scan is newer than the empty one
            publish(scannedOver, new ProjectIndex());
        } finally {
            rebuildLock.unlock();
        }
    }

    /**
     * Scans the repository and puts the mapping it finds in use.
     *
     * <p>One thread at a time rebuilds the mapping. The scan reads the repository without holding
     * {@link #indexLock}, so the current mapping stays readable meanwhile.
     *
     * <p>A save, a deletion or an added project may change the mapping during the scan. The scan is older than such a
     * change, so the repository is scanned again, and the change is kept.
     */
    private void rebuildIndex() throws IOException {
        rebuildLock.lock();
        try {
            ProjectIndexCache scannedOver;
            ProjectIndex index;
            do {
                scannedOver = indexCache.get();
                index = readExternalToInternalMap(delegate);
            } while (!publish(scannedOver, index));
        } finally {
            rebuildLock.unlock();
        }
    }

    /**
     * Puts the index in use, unless the mapping has changed since the one given was in use. The folders being saved
     * keep their new mapping.
     */
    private boolean publish(@Nullable ProjectIndexCache scannedOver, ProjectIndex index) {
        indexLock.writeLock().lock();
        try {
            foldersBeingMapped.forEach(folderData -> map(index, folderData));
            return indexCache.compareAndSet(scannedOver, new ProjectIndexCache(index));
        } finally {
            indexLock.writeLock().unlock();
        }
    }

    private ProjectIndex updateConfigFile(FileData folderData) {
        if (folderData.getAdditionalData(FileMappingData.class) == null) {
            log.warn("Unexpected behavior: FileMappingData is absent.");
            return getUpToDateMapping();
        }

        // We must ensure that our externalToInternal.getProjects() is up to date.
        refreshExpiredMapping();
        indexLock.writeLock().lock();
        try {
            var projectIndex = currentMappingCopy();
            map(projectIndex, folderData);

            // Update in-memory index
            indexCache.set(new ProjectIndexCache(projectIndex));
            return projectIndex;
        } finally {
            indexLock.writeLock().unlock();
        }
    }

    /** Maps the folder the data is written to under the external path of its {@link FileMappingData}. */
    private void map(ProjectIndex projectIndex, FileData folderData) {
        var mappingData = Objects.requireNonNull(folderData.getAdditionalData(FileMappingData.class));
        var externalPath = mappingData.getExternalPath();
        String projectName = externalPath.startsWith(baseFolder) ? externalPath.substring(baseFolder.length())
                : externalPath;
        findProject(projectIndex, folderData).ifPresentOrElse(project -> project.setName(projectName),
                () -> projectIndex.getProjects().add(new ProjectInfo(projectName, mappingData.getInternalPath())));
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

    private boolean isUpdateConfigNeeded(FileData folderData) {
        var mappingData = folderData.getAdditionalData(FileMappingData.class);
        if (mappingData != null) {
            var internalPath = mappingData.getInternalPath();
            var externalPath = baseFolder + getUpToDateMapping().getProjects()
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
        return toInternal(currentMapping(), externalPath);
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
        var mapping = getUpToDateMapping();
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

    /**
     * The hash a folder is named by, computed once per folder.
     *
     * <p>A repository holds far fewer folders than the cache admits. Beyond that the memo is starting to hold
     * folders nobody asks about any more, so it is dropped and filled again.
     */
    private String getHash(String path) {
        if (StringUtils.isEmpty(path)) {
            return "";
        }
        var hash = hashesByPath.get(path);
        if (hash == null) {
            hash = HashingUtils.sha256Hex(path);
            if (hashesByPath.size() >= FOLDER_HASH_CACHE_CAPACITY) {
                hashesByPath.clear();
            }
            hashesByPath.put(path, hash);
        }
        return hash;
    }

    @Override
    public boolean isBranchProtected(String branch) {
        return ((BranchRepository) delegate).isBranchProtected(branch);
    }
}
