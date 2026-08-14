package org.openl.studio.projects.service.files;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import org.openl.rules.common.ProjectException;
import org.openl.rules.project.abstraction.AProjectArtefact;
import org.openl.rules.project.abstraction.AProjectFolder;
import org.openl.rules.project.abstraction.AProjectResource;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.repository.api.ChangesetType;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.FileItem;
import org.openl.rules.rest.acl.service.AclProjectsHelper;
import org.openl.rules.webstudio.util.NameChecker;
import org.openl.studio.common.exception.BadRequestException;
import org.openl.studio.common.exception.ConflictException;
import org.openl.studio.common.exception.ForbiddenException;
import org.openl.studio.common.exception.NotFoundException;
import org.openl.studio.common.validation.BeanValidationProvider;
import org.openl.studio.common.validation.FileIntegrityValidator;
import org.openl.studio.projects.model.files.FolderNode;
import org.openl.studio.projects.model.files.FsNode;
import org.openl.studio.projects.validator.file.ProjectDescriptorValidator;
import org.openl.util.FileUtils;
import org.openl.util.StringUtils;

/**
 * Implementation of {@link ProjectFilesService}.
 *
 * <p>Listing and search walk the artefact tree iteratively, so a deeply nested mount does not exhaust
 * the stack. Copy and ZIP streaming recurse, bounded by the depth of the copied or zipped subtree.
 */
@Slf4j
@RequiredArgsConstructor
@Service
@Validated
public class ProjectFilesServiceImpl implements ProjectFilesService {

    /**
     * The size above which content is refused as a project descriptor. A {@code rules.xml} is a few
     * kilobytes; the cap keeps an oversized upload from being read into memory to be validated, and
     * from being written to the project unchecked.
     */
    private static final int MAX_DESCRIPTOR_SIZE = 16 * 1024 * 1024;

    private final AclProjectsHelper aclProjectsHelper;
    private final FileNodeMapper resourceMapper;
    private final FileSearchSupport searchSupport;
    private final FileArchiveSupport archiveSupport;
    private final ProjectDescriptorCleaner descriptorCleaner;
    private final BeanValidationProvider validationProvider;

    @Override
    public List<FsNode> getResources(@NotNull FileRoot root,
                                       @NotNull FileCriteriaQuery query,
                                       boolean recursive,
                                       @NotNull FileViewMode viewMode,
                                       String version) {
        root.requireReadable();
        AProjectFolder projectFolder = root.readFolder(version);
        AProjectFolder baseFolder = resolveBaseFolder(projectFolder, query);

        var filter = buildFilterCriteria(query);

        if (viewMode == FileViewMode.NESTED && recursive) {
            return buildNested(baseFolder, filter);
        } else {
            return buildFlatList(baseFolder, filter, recursive);
        }
    }

    @Override
    public AProjectResource getResource(@NotNull FileRoot root, @NotBlank String path, String version) {
        root.requireReadable();
        var resource = findFileArtefact(root.readFolder(version), path);
        requirePermission(resource, BasePermission.READ);
        return resource;
    }

    @Override
    public FsNode getNode(@NotNull FileRoot root, @NotBlank String path, String version) {
        return resourceMapper.map(getResource(root, path, version));
    }

    @Override
    public void updateResource(@NotNull FileRoot root,
                               @NotBlank String path,
                               @NotNull InputStream content) {
        root.requireModifiable();
        lockIfClosed(root);
        try {
            var resource = findFileArtefact(root.readFolder(null), path);
            requirePermission(resource, BasePermission.WRITE);
            // Validated before the project is reserved, so a rejected write leaves no lock behind. The
            // validated content is closed here as well, so a write that never happens leaves nothing behind.
            try (InputStream validatedContent = validateContent(root, path, content)) {
                lockForEditing(root, path);
                resource.setContent(validatedContent);
            }
        } catch (ProjectException | IOException e) {
            throw new ConflictException("file.update.failed.message");
        } finally {
            unlockIfClosed(root);
        }
    }

    @Override
    public void deleteResource(@NotNull FileRoot root, @NotBlank String path) {
        root.requireModifiable();
        validateResourcePath(path);
        lockIfClosed(root);
        try {
            AProjectArtefact found = findArtefactByPath(root.readFolder(null), path);
            if (found == null) {
                throw new NotFoundException("file.not.found.message");
            }
            requirePermission(found, BasePermission.DELETE);
            lockForEditing(root, path);
            if (root instanceof ProjectFileRoot projectRoot) {
                descriptorCleaner.unregisterModules(projectRoot.getProject(), found);
            }
            found.delete();
        } catch (ProjectException | IOException e) {
            throw new ConflictException("file.delete.failed.message");
        } finally {
            unlockIfClosed(root);
        }
    }

    @Override
    public void copyResource(@NotNull FileRoot root,
                             @NotBlank String sourcePath,
                             @NotBlank String destinationPath) {
        root.requireModifiable();
        validateResourcePath(destinationPath);
        lockIfClosed(root);
        try {
            var source = findExistingArtefact(root.readFolder(null), sourcePath);
            requirePermission(source, BasePermission.READ);
            requireNotPlacedIntoItself(source, sourcePath, destinationPath, "file.copy.into.itself.message");
            lockForEditing(root, destinationPath);
            var targetFolder = resolveOrCreateFolders(root.writeFolder(), destinationPath,
                    true, "file.copy.path.conflict.message");
            copyArtefact(source, targetFolder, FilePaths.name(destinationPath));
        } catch (ProjectException | IOException e) {
            throw new ConflictException("file.copy.failed.message");
        } finally {
            unlockIfClosed(root);
        }
    }

    @Override
    public void moveResource(@NotNull FileRoot root,
                             @NotBlank String sourcePath,
                             @NotBlank String destinationPath) {
        root.requireModifiable();
        validateResourcePath(destinationPath);
        lockIfClosed(root);
        try {
            var source = findExistingArtefact(root.readFolder(null), sourcePath);
            requirePermission(source, BasePermission.READ);
            requirePermission(source, BasePermission.DELETE);
            requireNotPlacedIntoItself(source, sourcePath, destinationPath, "file.move.into.itself.message");
            lockForEditing(root, sourcePath, destinationPath);
            var targetFolder = resolveOrCreateFolders(root.writeFolder(), destinationPath,
                    true, "file.move.path.conflict.message");
            String fileName = FilePaths.name(destinationPath);
            copyArtefact(source, targetFolder, fileName);
            deleteSourceOrRollback(source, targetFolder, fileName, destinationPath);
        } catch (ProjectException | IOException e) {
            throw new ConflictException("file.move.failed.message");
        } finally {
            unlockIfClosed(root);
        }
    }

    @Override
    public void createResource(@NotNull FileRoot root,
                               @NotBlank String path,
                               @NotNull InputStream content,
                               boolean createFolders) {
        root.requireModifiable();
        validateResourcePath(path);
        lockIfClosed(root);
        try {
            // An opened project is reserved only once the content is known to be writable, so a rejected
            // write leaves no lock behind. A closed project is reserved beforehand, as it is for the other
            // modifications, so the content is not validated against a state another user is changing.
            try (InputStream validatedContent = validateContent(root, path, content)) {
                lockForEditing(root, path);
                var targetFolder = resolveOrCreateFolders(root.writeFolder(), path,
                        createFolders, "file.path.not.folder.message");
                targetFolder.addResource(FilePaths.name(path), validatedContent);
            }
        } catch (ProjectException | IOException e) {
            throw new ConflictException("file.create.failed.message");
        } finally {
            unlockIfClosed(root);
        }
    }

    @Override
    public void createFolder(@NotNull FileRoot root, @NotBlank String path, boolean createParents) {
        root.requireModifiable();
        validateResourcePath(path);
        lockForEditing(root, path);
        String[] segments = path.split("/");
        try {
            AProjectFolder current = root.writeFolder();
            for (int i = 0; i < segments.length; i++) {
                String segment = segments[i];
                if (current.hasArtefact(segment)) {
                    AProjectArtefact artefact = current.getArtefact(segment);
                    if (!artefact.isFolder()) {
                        throw new ConflictException("file.path.not.folder.message", artefact.getInternalPath());
                    }
                    current = (AProjectFolder) artefact;
                } else {
                    if (!createParents && i < segments.length - 1) {
                        throw new NotFoundException("file.parent.not.found.message", segment);
                    }
                    requirePermission(current, BasePermission.CREATE);
                    current = current.addFolder(segment);
                }
            }
        } catch (ProjectException e) {
            throw new ConflictException("file.create.failed.message");
        } finally {
            unlockIfClosed(root);
        }
    }

    @Override
    public void writeFolderAsZip(@NotNull FileRoot root, String path, @NotNull OutputStream out,
                                 String version) throws IOException {
        root.requireReadable();
        AProjectFolder folder = root.readFolder(version);
        // A blank path zips the whole project (the root folder), e.g. project export.
        AProjectArtefact artefact = StringUtils.isBlank(path) ? folder : findExistingArtefact(folder, path);
        if (!artefact.isFolder()) {
            throw new BadRequestException("file.base-path.not-folder.message", new Object[]{path});
        }
        requirePermission(artefact, BasePermission.READ);
        archiveSupport.writeZip((AProjectFolder) artefact, out);
    }

    @Override
    public void uploadArchive(@NotNull FileRoot root,
                              @NotNull String path,
                              @NotNull InputStream archive,
                              boolean createParents,
                              @NotNull ConflictPolicy conflictPolicy) throws IOException {
        root.requireModifiable();
        requireWritableBase(root, path);
        writeEntries(root, path, archiveSupport.readArchive(path, archive), conflictPolicy,
                uploadComment("Upload archive to ", path));
    }

    @Override
    public void uploadFiles(@NotNull FileRoot root,
                            @NotNull String path,
                            @NotNull List<UploadedFile> files,
                            @NotNull ConflictPolicy conflictPolicy) {
        root.requireModifiable();
        requireWritableBase(root, path);
        List<FileEntry> entries = new ArrayList<>();
        for (UploadedFile file : files) {
            String name = FilePaths.stripLeadingSlashes(StringUtils.trimToEmpty(file.name()).replace('\\', '/'));
            if (name.isEmpty()) {
                throw new BadRequestException("file.path.invalid.message");
            }
            String fullPath = path.isEmpty() ? name : path + "/" + name;
            entries.add(new FileEntry(fullPath, file.content()));
        }
        writeEntries(root, path, entries, conflictPolicy, uploadComment("Upload files to ", path));
    }

    /**
     * Validates the target folder and verifies it may be created or written, unless it is the mount root.
     */
    private void requireWritableBase(FileRoot root, String path) {
        if (!path.isEmpty()) {
            validateResourcePath(path);
            requirePermission(root.writeFolder(), BasePermission.CREATE);
        }
    }

    /**
     * Commits the entries to the mount as one changeset. Every entry path is validated first,
     * rejecting zip-slip and other unsafe names before anything is written.
     *
     * <p>For {@code FAIL}, {@code SKIP} and {@code OVERWRITE} the changeset only adds and
     * overwrites files, honoring the policy per entry; nothing is committed when every entry is
     * skipped. For {@code REPLACE} the base folder is made to contain exactly the uploaded
     * entries, and the user must be allowed to delete every file the replace removes.
     */
    private void writeEntries(FileRoot root,
                              String basePath,
                              List<FileEntry> entries,
                              ConflictPolicy conflictPolicy,
                              String comment) {
        if (conflictPolicy == ConflictPolicy.REPLACE && entries.isEmpty()) {
            throw new BadRequestException("file.archive.empty.message");
        }
        entries.forEach(entry -> validateResourcePath(entry.fullPath()));
        AProjectFolder current = root.readFolder(null);
        List<FileItem> items = new ArrayList<>();
        for (FileEntry entry : entries) {
            if (!isEntrySkipped(current, entry, conflictPolicy)) {
                // An upload brings its own files with it - the libraries a descriptor names among them - so
                // its descriptor is not checked against the working copy the upload is about to replace.
                verifyFileContent(FilePaths.name(entry.fullPath()), entry.data());
                var fileData = new FileData();
                fileData.setName(entry.fullPath());
                items.add(new FileItem(fileData, new ByteArrayInputStream(entry.data())));
            }
        }
        if (conflictPolicy == ConflictPolicy.REPLACE) {
            requireRemovedFilesDeletable(current, basePath, entries);
            root.writeBatch(basePath, items, ChangesetType.FULL, comment);
        } else if (!items.isEmpty()) {
            root.writeBatch(basePath, items, ChangesetType.DIFF, comment);
        }
    }

    /**
     * Applies the per-entry conflict policy to an entry whose target file already exists: reports
     * a conflict for {@code FAIL}, drops the entry for {@code SKIP}, and keeps it for
     * {@code OVERWRITE} and {@code REPLACE}.
     */
    private boolean isEntrySkipped(AProjectFolder current, FileEntry entry, ConflictPolicy conflictPolicy) {
        if (findArtefactByPath(current, entry.fullPath()) == null) {
            return false;
        }
        return switch (conflictPolicy) {
            case FAIL -> throw new ConflictException("file.archive.entry.exists.message", entry.fullPath());
            case SKIP -> true;
            case OVERWRITE, REPLACE -> false;
        };
    }

    /**
     * Verifies the user may delete every file that the full replace removes: the files under the
     * base folder that are not part of the upload.
     */
    private void requireRemovedFilesDeletable(AProjectFolder current, String basePath, List<FileEntry> entries) {
        AProjectArtefact base = basePath.isEmpty() ? current : findArtefactByPath(current, basePath);
        if (!(base instanceof AProjectFolder folder)) {
            return;
        }
        Set<String> kept = entries.stream().map(FileEntry::fullPath).collect(Collectors.toSet());
        Deque<AProjectFolder> queue = new ArrayDeque<>();
        queue.add(folder);
        while (!queue.isEmpty()) {
            for (AProjectArtefact artefact : queue.poll().getArtefacts()) {
                if (artefact.isFolder()) {
                    queue.add((AProjectFolder) artefact);
                } else if (!kept.contains(artefact.getInternalPath())) {
                    requirePermission(artefact, BasePermission.DELETE);
                }
            }
        }
    }

    private static String uploadComment(String action, String path) {
        return action + (path.isEmpty() ? "repository root" : path);
    }

    @Override
    public List<FsNode> search(@NotNull FileRoot root, @NotNull FileSearchQuery query) {
        return searchSupport.search(root, query);
    }

    /**
     * Deletes the source artefact. If deletion fails, rolls back by removing the already-copied resource.
     */
    private void deleteSourceOrRollback(AProjectArtefact source,
                                        AProjectFolder targetFolder,
                                        String fileName,
                                        String destinationPath) throws ProjectException {
        try {
            source.delete();
        } catch (ProjectException deleteEx) {
            try {
                targetFolder.getArtefact(fileName).delete();
            } catch (ProjectException rollbackEx) {
                log.error("Failed to rollback copied resource '{}' after move failure", destinationPath, rollbackEx);
            }
            throw deleteEx;
        }
    }

    /**
     * Checks that the user has the specified permission on the artefact.
     */
    private void requirePermission(AProjectArtefact artefact, org.springframework.security.acls.model.Permission permission) {
        if (!aclProjectsHelper.hasPermission(artefact, permission)) {
            throw new ForbiddenException("default.message");
        }
    }

    /**
     * Reserves a mount for a modification of the given paths, so a lock conflict is reported
     * precisely before anything is written.
     *
     * <p>A project mount reserves its whole project for editing; the paths play no role. A
     * repository mount verifies that no project owning one of the paths is locked by another
     * user; the write itself is not serialized, matching direct repository commits.
     *
     * @throws ConflictException when an affected project is locked by another user
     */
    private static void lockForEditing(FileRoot root, String... paths) {
        if (root instanceof ProjectFileRoot projectRoot) {
            projectRoot.lockForEditing();
        } else if (root instanceof RepoFileRoot repoRoot) {
            repoRoot.requireUnlocked(List.of(paths));
        }
    }

    /**
     * Reserves a closed project before the artefacts of a modification are resolved: its state
     * lives in the design repository, which other users change in parallel, so the resolved
     * artefacts must not change until the modification ends. An opened project is resolved from
     * the user's own working copy and is locked later, after validation.
     *
     * @throws ConflictException when the project is locked by another user
     */
    private static void lockIfClosed(FileRoot root) {
        if (root instanceof ProjectFileRoot projectRoot) {
            projectRoot.lockIfClosed();
        }
    }

    /**
     * Releases the edit lock of a project-backed mount when the modification leaves nothing for it
     * to guard: a closed project is committed directly to the design repository, so it must not
     * stay locked. An opened project keeps the lock until it is saved or closed.
     */
    private static void unlockIfClosed(FileRoot root) {
        if (root instanceof ProjectFileRoot projectRoot) {
            projectRoot.unlockIfClosed();
        }
    }

    /**
     * Validates path, locates a file artefact, and verifies it exists and is not a folder.
     */
    private AProjectResource findFileArtefact(AProjectFolder root, String path) {
        validateResourcePath(path);
        AProjectArtefact found = findArtefactByPath(root, path);
        if (found == null || found.isFolder()) {
            throw new NotFoundException("file.not.found.message");
        }
        return (AProjectResource) found;
    }

    /**
     * Validates the path and locates an artefact, which may be a file or a folder.
     *
     * @throws BadRequestException if the path is invalid
     * @throws NotFoundException   if nothing exists at the path
     */
    private AProjectArtefact findExistingArtefact(AProjectFolder root, String path) {
        validateResourcePath(path);
        AProjectArtefact found = findArtefactByPath(root, path);
        if (found == null) {
            throw new NotFoundException("file.not.found.message");
        }
        return found;
    }

    /**
     * Rejects copying or moving a folder into itself or into one of its own descendants.
     * Such a destination would otherwise cause the recursive copy to never finish.
     */
    private static void requireNotPlacedIntoItself(AProjectArtefact source,
                                                   String sourcePath,
                                                   String destinationPath,
                                                   String messageKey) {
        if (source.isFolder()
                && (destinationPath.equals(sourcePath) || destinationPath.startsWith(sourcePath + "/"))) {
            throw new ConflictException(messageKey, sourcePath);
        }
    }

    /**
     * Copies an artefact into the target folder under the given name. A file is copied by
     * content. A folder is replicated together with all of its descendants.
     */
    private void copyArtefact(AProjectArtefact source, AProjectFolder targetFolder, String name)
            throws ProjectException, IOException {
        if (source.isFolder()) {
            AProjectFolder created = targetFolder.addFolder(name);
            for (AProjectArtefact child : ((AProjectFolder) source).getArtefacts()) {
                copyArtefact(child, created, child.getName());
            }
        } else {
            try (var content = ((AProjectResource) source).getContent()) {
                targetFolder.addResource(name, content);
            }
        }
    }

    /**
     * Resolves existing parent folders of the given path within the project, optionally creating
     * missing intermediate folders. Checks CREATE permission on the deepest existing folder.
     *
     * @param rootFolder         the writable root folder
     * @param fullPath           full resource path including file name
     * @param createMissing      if {@code true}, create missing intermediate folders;
     *                           if {@code false}, throw {@link NotFoundException}
     * @param conflictMessageKey error message key when a path segment is not a folder
     * @return the parent folder where the resource should be placed
     */
    private AProjectFolder resolveOrCreateFolders(AProjectFolder rootFolder,
                                                  String fullPath,
                                                  boolean createMissing,
                                                  String conflictMessageKey) throws ProjectException {
        String[] segments = fullPath.split("/");
        AProjectFolder targetFolder = rootFolder;
        int firstMissing = segments.length - 1;

        for (int i = 0; i < segments.length - 1; i++) {
            String segment = segments[i];
            if (!targetFolder.hasArtefact(segment)) {
                if (!createMissing) {
                    throw new NotFoundException("file.parent.not.found.message", segment);
                }
                firstMissing = i;
                break;
            }
            AProjectArtefact artefact = targetFolder.getArtefact(segment);
            if (!artefact.isFolder()) {
                throw new ConflictException(conflictMessageKey, artefact.getInternalPath());
            }
            targetFolder = (AProjectFolder) artefact;
        }

        requirePermission(targetFolder, BasePermission.CREATE);

        for (int i = firstMissing; i < segments.length - 1; i++) {
            targetFolder = targetFolder.addFolder(segments[i]);
        }

        return targetFolder;
    }

    /**
     * Validates the content written to a path.
     *
     * <p>The project descriptor of a project is checked for settings the engine cannot use. Any
     * other file is checked against the format its extension promises.
     *
     * @return a stream positioned at the beginning (after validation), to be closed by the caller
     */
    private InputStream validateContent(FileRoot root, String path, InputStream content) {
        // The path arrives as the request wrote it, so the surrounding slashes go before it names a file.
        var filePath = FilePaths.trimSlashes(path);
        if (root instanceof ProjectFileRoot projectRoot && ProjectDescriptor.FILE_NAME.equals(filePath)) {
            return validateDescriptor(projectRoot, content);
        }
        return verifyFileContent(FilePaths.name(filePath), content);
    }

    /**
     * Validates the project descriptor written to a project against the one the project stores.
     *
     * <p>A descriptor that is not well-formed XML is written as it is, so a broken file can always be
     * replaced by a fixed one. One larger than a descriptor can be is refused instead of written
     * unchecked.
     *
     * @return a stream positioned at the beginning (after validation)
     */
    private InputStream validateDescriptor(ProjectFileRoot root, InputStream content) {
        byte[] declared;
        // The content is read in full, so the stream the caller opened is closed here rather than by the write.
        try (content) {
            declared = content.readNBytes(MAX_DESCRIPTOR_SIZE + 1);
        } catch (IOException e) {
            throw new BadRequestException("file.content.invalid.message");
        }
        if (declared.length > MAX_DESCRIPTOR_SIZE) {
            throw new BadRequestException("file.descriptor.too-large.message");
        }
        var descriptor = ProjectDescriptor.read(new ByteArrayInputStream(declared));
        if (descriptor != null) {
            validationProvider.validate(descriptor,
                    ProjectDescriptorValidator.forProject(root.getProject(), storedDescriptor(root)));
        }
        return new ByteArrayInputStream(declared);
    }

    /**
     * The descriptor the project stores now, or {@code null} when it has none or cannot be read.
     */
    private @Nullable ProjectDescriptor storedDescriptor(FileRoot root) {
        var artefact = findArtefactByPath(root.readFolder(null), ProjectDescriptor.FILE_NAME);
        if (!(artefact instanceof AProjectResource resource)) {
            return null;
        }
        try (var content = resource.getContent()) {
            return ProjectDescriptor.read(content);
        } catch (ProjectException | IOException e) {
            // Without the stored settings the write is checked as if it introduced all of them, so the
            // failure is worth reporting: it is what a rejection of an untouched setting would come from.
            log.warn("Failed to read the stored '{}'.", ProjectDescriptor.FILE_NAME, e);
            return null;
        }
    }

    /**
     * Verifies that the uploaded content arrived complete and in the format its extension promises.
     *
     * <p>A workbook or an archive is checked against the structure the format records about itself,
     * so content that was cut short is refused instead of being stored as a rule module nobody can
     * open. A file of any other type is written as it arrives.
     *
     * @return a stream of the verified content, positioned at the beginning; it must be closed by
     * the caller, because a verified stream holds a temporary copy of the content
     */
    private InputStream verifyFileContent(String fileName, InputStream content) {
        try {
            return FileIntegrityValidator.verify(fileName, content);
        } catch (IOException e) {
            throw FileIntegrityValidator.damagedContent(fileName, e);
        }
    }

    /**
     * Verifies content already held in memory. Unlike {@link #verifyFileContent(String, InputStream)}
     * it needs no temporary copy, so nothing is left for the caller to close.
     */
    private void verifyFileContent(String fileName, byte[] content) {
        try {
            FileIntegrityValidator.verify(fileName, content);
        } catch (IOException e) {
            throw FileIntegrityValidator.damagedContent(fileName, e);
        }
    }

    /**
     * Validates a resource path using {@link NameChecker#validatePath(String)}.
     *
     * @throws BadRequestException if the path is invalid
     */
    private void validateResourcePath(String path) {
        try {
            NameChecker.validatePath(path);
        } catch (IOException e) {
            throw new BadRequestException("file.path.invalid.message");
        }
    }

    private AProjectArtefact findArtefactByPath(AProjectFolder rootFolder, String path) {
        String[] segments = path.split("/");
        AProjectArtefact current = rootFolder;
        for (String segment : segments) {
            if (!current.isFolder()) {
                return null;
            }
            try {
                current = ((AProjectFolder) current).getArtefact(segment);
            } catch (ProjectException e) {
                return null;
            }
        }
        return current;
    }

    private List<FsNode> buildNested(AProjectFolder rootFolder,
                                       Predicate<AProjectArtefact> filter) {
        var builtChildren = new IdentityHashMap<AProjectFolder, List<FsNode>>();
        record Frame(AProjectFolder folder, boolean expanded) {}

        Deque<Frame> stack = new ArrayDeque<>();
        stack.push(new Frame(rootFolder, false));

        while (!stack.isEmpty()) {
            var frame = stack.pop();
            var folder = frame.folder();

            if (!frame.expanded()) {
                stack.push(new Frame(folder, true));
                for (var artefact : folder.getArtefacts()) {
                    if (artefact.isFolder()) {
                        stack.push(new Frame((AProjectFolder) artefact, false));
                    }
                }
            } else {
                var children = buildNestedChildren(folder, filter, builtChildren);
                children.sort(FileNodeMapper.NODE_COMPARATOR);
                builtChildren.put(folder, children);
            }
        }

        return builtChildren.getOrDefault(rootFolder, List.of());
    }

    private List<FsNode> buildNestedChildren(AProjectFolder folder,
                                               Predicate<AProjectArtefact> filter,
                                               IdentityHashMap<AProjectFolder, List<FsNode>> builtChildren) {
        List<FsNode> out = new ArrayList<>();
        for (var artefact : folder.getArtefacts()) {
            if (!artefact.isFolder()) {
                if (filter.test(artefact)) {
                    out.add(resourceMapper.map(artefact));
                }
                continue;
            }
            var childChildren = builtChildren.getOrDefault((AProjectFolder) artefact, List.of());
            if (!filter.test(artefact) && childChildren.isEmpty()) {
                continue;
            }
            FsNode mapped = resourceMapper.map(artefact);
            if (mapped instanceof FolderNode fr && !childChildren.isEmpty()) {
                // childChildren was already sorted when it was stored, so no need to sort again.
                mapped = fr.withChildren(childChildren);
            }
            out.add(mapped);
        }
        return out;
    }

    private AProjectFolder resolveBaseFolder(AProjectFolder folder, FileCriteriaQuery query) {
        if (StringUtils.isBlank(query.basePath())) {
            return folder;
        }
        AProjectArtefact artefact = null;
        try {
            for (var segment : query.basePath().split("/")) {
                artefact = folder.getArtefact(segment);
                if (artefact == null || !artefact.isFolder()) {
                    artefact = null;
                    break;
                }
                folder = (AProjectFolder) artefact;
            }
        } catch (ProjectException e) {
            log.debug("Failed to resolve base folder path '{}'", query.basePath(), e);
            artefact = null;
        }
        if (artefact == null || !artefact.isFolder()) {
            throw new BadRequestException("file.base-path.not-folder.message", new Object[]{query.basePath()});
        }
        return (AProjectFolder) artefact;
    }

    /**
     * Builds a filter predicate based on the query criteria and ACL permissions.
     * The filter is applied before mapping to DTO to minimize overhead.
     */
    private Predicate<AProjectArtefact> buildFilterCriteria(FileCriteriaQuery query) {
        Predicate<AProjectArtefact> filter = artefact -> true;

        // Folders only filter
        if (query.foldersOnly()) {
            filter = filter.and(AProjectArtefact::isFolder);
        }

        // Name pattern filter (case-insensitive contains)
        if (StringUtils.isNotBlank(query.namePattern())) {
            var pattern = query.namePattern().toLowerCase();
            filter = filter.and(artefact -> artefact.getName().toLowerCase().contains(pattern));
        }

        // Extension filter (only applies to files, folders always pass to preserve tree structure)
        if (!query.extensions().isEmpty()) {
            Set<String> normalizedExtensions = query.extensions().stream()
                    .map(String::toLowerCase)
                    .collect(Collectors.toSet());
            filter = filter.and(artefact -> {
                if (artefact.isFolder()) {
                    return true; // Folders always pass extension filter
                }
                var ext = FileUtils.getExtension(artefact.getName());
                return ext != null && normalizedExtensions.contains(ext.toLowerCase());
            });
        }

        // permissions filter must always be the last to minimize effort on ACL because it's quite expensive
        return filter.and(artefact -> aclProjectsHelper.hasPermission(artefact, BasePermission.READ));
    }

    /**
     * Builds a flat list of resources using iterative queue-based traversal.
     */
    private List<FsNode> buildFlatList(AProjectFolder rootFolder,
                                         Predicate<AProjectArtefact> filter,
                                         boolean recursive) {
        List<FsNode> result = new ArrayList<>();

        Deque<AProjectFolder> queue = new ArrayDeque<>();
        queue.add(rootFolder);

        while (!queue.isEmpty()) {
            AProjectFolder folder = queue.poll();
            for (AProjectArtefact artefact : folder.getArtefacts()) {
                if (filter.test(artefact)) {
                    result.add(resourceMapper.map(artefact));
                }

                if (recursive && artefact.isFolder()) {
                    queue.add((AProjectFolder) artefact);
                }
            }
        }

        result.sort(FileNodeMapper.NODE_COMPARATOR);
        return result;
    }


}
