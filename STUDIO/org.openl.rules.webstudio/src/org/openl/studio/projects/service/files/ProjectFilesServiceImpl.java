package org.openl.studio.projects.service.files;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
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
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import org.openl.rules.common.ProjectException;
import org.openl.rules.project.abstraction.AProjectArtefact;
import org.openl.rules.project.abstraction.AProjectFolder;
import org.openl.rules.project.abstraction.AProjectResource;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.FileItem;
import org.openl.rules.rest.acl.service.AclProjectsHelper;
import org.openl.rules.webstudio.util.NameChecker;
import org.openl.studio.common.exception.BadRequestException;
import org.openl.studio.common.exception.ConflictException;
import org.openl.studio.common.exception.ForbiddenException;
import org.openl.studio.common.exception.NotFoundException;
import org.openl.studio.projects.model.files.FolderNode;
import org.openl.studio.projects.model.files.FsNode;
import org.openl.util.FileSignatureHelper;
import org.openl.util.FileTypeHelper;
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

    private final AclProjectsHelper aclProjectsHelper;
    private final FileNodeMapper resourceMapper;
    private final FileSearchSupport searchSupport;
    private final FileArchiveSupport archiveSupport;

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
        var resource = findFileArtefact(root.readFolder(null), path);
        requirePermission(resource, BasePermission.WRITE);
        InputStream validatedContent = validateContent(resource.getName(), content);
        try {
            resource.setContent(validatedContent);
        } catch (ProjectException e) {
            throw new ConflictException("file.update.failed.message");
        }
    }

    @Override
    public void deleteResource(@NotNull FileRoot root, @NotBlank String path) {
        root.requireModifiable();
        validateResourcePath(path);
        AProjectArtefact found = findArtefactByPath(root.readFolder(null), path);
        if (found == null) {
            throw new NotFoundException("file.not.found.message");
        }
        requirePermission(found, BasePermission.DELETE);
        try {
            found.delete();
        } catch (ProjectException e) {
            throw new ConflictException("file.delete.failed.message");
        }
    }

    @Override
    public void copyResource(@NotNull FileRoot root,
                             @NotBlank String sourcePath,
                             @NotBlank String destinationPath) {
        root.requireModifiable();
        validateResourcePath(destinationPath);
        var source = findExistingArtefact(root.readFolder(null), sourcePath);
        requirePermission(source, BasePermission.READ);
        requireNotPlacedIntoItself(source, sourcePath, destinationPath, "file.copy.into.itself.message");

        try {
            var targetFolder = resolveOrCreateFolders(root.writeFolder(), destinationPath,
                    true, "file.copy.path.conflict.message");
            copyArtefact(source, targetFolder, FilePaths.name(destinationPath));
        } catch (ProjectException | IOException e) {
            throw new ConflictException("file.copy.failed.message");
        }
    }

    @Override
    public void moveResource(@NotNull FileRoot root,
                             @NotBlank String sourcePath,
                             @NotBlank String destinationPath) {
        root.requireModifiable();
        validateResourcePath(destinationPath);
        var source = findExistingArtefact(root.readFolder(null), sourcePath);
        requirePermission(source, BasePermission.READ);
        requirePermission(source, BasePermission.DELETE);
        requireNotPlacedIntoItself(source, sourcePath, destinationPath, "file.move.into.itself.message");

        try {
            var targetFolder = resolveOrCreateFolders(root.writeFolder(), destinationPath,
                    true, "file.move.path.conflict.message");
            String fileName = FilePaths.name(destinationPath);
            copyArtefact(source, targetFolder, fileName);
            deleteSourceOrRollback(source, targetFolder, fileName, destinationPath);
        } catch (ProjectException | IOException e) {
            throw new ConflictException("file.move.failed.message");
        }
    }

    @Override
    public void createResource(@NotNull FileRoot root,
                               @NotBlank String path,
                               @NotNull InputStream content,
                               boolean createFolders) {
        root.requireModifiable();
        validateResourcePath(path);

        try {
            var targetFolder = resolveOrCreateFolders(root.writeFolder(), path,
                    createFolders, "file.path.not.folder.message");
            String fileName = FilePaths.name(path);
            InputStream validatedContent = validateContent(fileName, content);
            targetFolder.addResource(fileName, validatedContent);
        } catch (ProjectException e) {
            throw new ConflictException("file.create.failed.message");
        }
    }

    @Override
    public void createFolder(@NotNull FileRoot root, @NotBlank String path, boolean createParents) {
        root.requireModifiable();
        validateResourcePath(path);
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
        }
    }

    @Override
    public void writeFolderAsZip(@NotNull FileRoot root, @NotBlank String path, @NotNull OutputStream out,
                                 String version) throws IOException {
        root.requireReadable();
        AProjectArtefact artefact = findExistingArtefact(root.readFolder(version), path);
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
        writeEntries(root, archiveSupport.readArchive(path, archive), conflictPolicy,
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
        writeEntries(root, entries, conflictPolicy, uploadComment("Upload files to ", path));
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
     * Writes the entries to the mount: a single changeset on an atomic (repository) mount, file by
     * file otherwise. Every entry path is validated first, rejecting zip-slip and other unsafe names
     * before anything is written.
     */
    private void writeEntries(FileRoot root, List<FileEntry> entries, ConflictPolicy conflictPolicy, String comment) {
        entries.forEach(entry -> validateResourcePath(entry.fullPath()));
        if (root.supportsAtomicWrite()) {
            writeEntriesAtomically(root, entries, conflictPolicy, comment);
        } else {
            AProjectFolder writeFolder = root.writeFolder();
            try {
                for (FileEntry entry : entries) {
                    addFileEntry(writeFolder, entry.fullPath(), entry.data(), conflictPolicy);
                }
            } catch (ProjectException e) {
                throw new ConflictException("file.create.failed.message");
            }
        }
    }

    /**
     * Validates and commits every archive entry as a single changeset, honoring the conflict policy.
     * The mount overwrites existing files only for the {@code OVERWRITE} policy.
     */
    private void writeEntriesAtomically(FileRoot root,
                                        List<FileEntry> entries,
                                        ConflictPolicy conflictPolicy,
                                        String comment) {
        AProjectFolder current = root.readFolder(null);
        List<FileItem> items = new ArrayList<>();
        for (FileEntry entry : entries) {
            if (findArtefactByPath(current, entry.fullPath()) != null) {
                switch (conflictPolicy) {
                    case FAIL -> throw new ConflictException("file.archive.entry.exists.message", entry.fullPath());
                    case SKIP -> {
                        continue;
                    }
                    case OVERWRITE -> {
                        // kept: the changeset overwrites the existing file
                    }
                }
            }
            InputStream validated = validateContent(FilePaths.name(entry.fullPath()), new ByteArrayInputStream(entry.data()));
            var fileData = new FileData();
            fileData.setName(entry.fullPath());
            items.add(new FileItem(fileData, validated));
        }
        root.writeBatch(items, comment);
    }

    private static String uploadComment(String action, String path) {
        return action + (path.isEmpty() ? "repository root" : path);
    }

    /**
     * Validates one archive entry and writes it into the target folder, honoring the conflict policy.
     */
    private void addFileEntry(AProjectFolder writeFolder, String fullPath, byte[] data, ConflictPolicy conflictPolicy)
            throws ProjectException {
        String fileName = FilePaths.name(fullPath);
        InputStream validated = validateContent(fileName, new ByteArrayInputStream(data));
        AProjectFolder targetFolder = resolveOrCreateFolders(writeFolder, fullPath, true, "file.path.not.folder.message");
        if (targetFolder.hasArtefact(fileName)) {
            switch (conflictPolicy) {
                case FAIL -> throw new ConflictException("file.archive.entry.exists.message", fullPath);
                case SKIP -> {
                    return;
                }
                case OVERWRITE -> targetFolder.getArtefact(fileName).delete();
            }
        }
        targetFolder.addResource(fileName, validated);
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
     * Validates that the uploaded content is consistent with the file extension.
     * For Excel files (.xlsx, .xlsm), validates the ZIP file signature.
     * For legacy Excel files (.xls), validates the OLE2 compound document signature.
     *
     * @return a buffered stream positioned at the beginning (after validation)
     */
    private InputStream validateContent(String fileName, InputStream content) {
        if (!FileTypeHelper.isExcelFile(fileName)) {
            return content;
        }
        var buffered = new BufferedInputStream(content);
        try {
            buffered.mark(4);
            int sign = new DataInputStream(buffered).readInt();
            buffered.reset();

            String lcName = fileName.toLowerCase();
            // A ".xls" suffix is legacy OLE2; ".xlsx"/".xlsm" are ZIP-based and cannot end with ".xls".
            if (lcName.endsWith(".xls")) {
                if (!FileSignatureHelper.isOle2Sign(sign)) {
                    throw new BadRequestException("file.content.invalid.message");
                }
            } else {
                if (!FileSignatureHelper.isArchiveSign(sign)) {
                    throw new BadRequestException("file.content.invalid.message");
                }
            }
        } catch (IOException e) {
            throw new BadRequestException("file.content.invalid.message");
        }
        return buffered;
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
