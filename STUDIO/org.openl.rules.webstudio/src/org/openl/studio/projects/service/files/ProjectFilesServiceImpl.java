package org.openl.studio.projects.service.files;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
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
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.rest.acl.service.AclProjectsHelper;
import org.openl.rules.webstudio.util.NameChecker;
import org.openl.studio.common.exception.BadRequestException;
import org.openl.studio.common.exception.ConflictException;
import org.openl.studio.common.exception.ForbiddenException;
import org.openl.studio.common.exception.NotFoundException;
import org.openl.studio.projects.model.files.FolderNode;
import org.openl.studio.projects.model.files.FsNode;
import org.openl.studio.projects.validator.ProjectStateValidator;
import org.openl.util.FileSignatureHelper;
import org.openl.util.FileTypeHelper;
import org.openl.util.FileUtils;
import org.openl.util.StringUtils;

/**
 * Implementation of {@link ProjectFilesService}.
 * Uses iterative queue-based traversal instead of recursion to avoid stack overflow.
 */
@Slf4j
@RequiredArgsConstructor
@Service
@Validated
public class ProjectFilesServiceImpl implements ProjectFilesService {

    private final AclProjectsHelper aclProjectsHelper;
    private final ProjectStateValidator projectStateValidator;
    private final FileNodeMapper resourceMapper;

    @Override
    public List<FsNode> getResources(@NotNull RulesProject project,
                                       @NotNull FileCriteriaQuery query,
                                       boolean recursive,
                                       @NotNull FileViewMode viewMode) {
        // Verify user has READ permission on the project
        if (!aclProjectsHelper.hasPermission(project, BasePermission.READ)) {
            throw new ForbiddenException("default.message");
        }
        AProjectFolder projectFolder = convertToFolder(project);
        AProjectFolder baseFolder = resolveBaseFolder(projectFolder, query);

        var filter = buildFilterCriteria(query);

        if (viewMode == FileViewMode.NESTED && recursive) {
            return buildNested(baseFolder, filter);
        } else {
            return buildFlatList(baseFolder, filter, recursive);
        }
    }

    @Override
    public AProjectResource getResource(@NotNull RulesProject project, @NotBlank String path) {
        requirePermission(project, BasePermission.READ);
        var resource = findFileArtefact(project, path);
        requirePermission(resource, BasePermission.READ);
        return resource;
    }

    @Override
    public FsNode getNode(@NotNull RulesProject project, @NotBlank String path) {
        return resourceMapper.map(getResource(project, path));
    }

    @Override
    public void updateResource(@NotNull RulesProject project,
                               @NotBlank String path,
                               @NotNull InputStream content) {
        requireModifiable(project);
        var resource = findFileArtefact(project, path);
        requirePermission(resource, BasePermission.WRITE);
        InputStream validatedContent = validateContent(resource.getName(), content);
        try {
            resource.setContent(validatedContent);
        } catch (ProjectException e) {
            throw new ConflictException("file.update.failed.message");
        }
    }

    @Override
    public void deleteResource(@NotNull RulesProject project, @NotBlank String path) {
        requireModifiable(project);
        validateResourcePath(path);
        AProjectArtefact found = findArtefactByPath(convertToFolder(project), path);
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
    public void copyResource(@NotNull RulesProject project,
                             @NotBlank String sourcePath,
                             @NotBlank String destinationPath) {
        requireModifiable(project);
        validateResourcePath(destinationPath);
        var source = findExistingArtefact(project, sourcePath);
        requirePermission(source, BasePermission.READ);
        requireNotPlacedIntoItself(source, sourcePath, destinationPath, "file.copy.into.itself.message");

        try {
            var targetFolder = resolveOrCreateFolders(project, destinationPath,
                    true, "file.copy.path.conflict.message");
            copyArtefact(source, targetFolder, getFileName(destinationPath));
        } catch (ProjectException | IOException e) {
            throw new ConflictException("file.copy.failed.message");
        }
    }

    @Override
    public void moveResource(@NotNull RulesProject project,
                             @NotBlank String sourcePath,
                             @NotBlank String destinationPath) {
        requireModifiable(project);
        validateResourcePath(destinationPath);
        var source = findExistingArtefact(project, sourcePath);
        requirePermission(source, BasePermission.READ);
        requirePermission(source, BasePermission.DELETE);
        requireNotPlacedIntoItself(source, sourcePath, destinationPath, "file.move.into.itself.message");

        try {
            var targetFolder = resolveOrCreateFolders(project, destinationPath,
                    true, "file.move.path.conflict.message");
            String fileName = getFileName(destinationPath);
            copyArtefact(source, targetFolder, fileName);
            deleteSourceOrRollback(source, targetFolder, fileName, destinationPath);
        } catch (ProjectException | IOException e) {
            throw new ConflictException("file.move.failed.message");
        }
    }

    @Override
    public void createResource(@NotNull RulesProject project,
                               @NotBlank String path,
                               @NotNull InputStream content,
                               boolean createFolders) {
        requireModifiable(project);
        validateResourcePath(path);

        try {
            var targetFolder = resolveOrCreateFolders(project, path,
                    createFolders, "file.path.not.folder.message");
            String fileName = getFileName(path);
            InputStream validatedContent = validateContent(fileName, content);
            targetFolder.addResource(fileName, validatedContent);
        } catch (ProjectException e) {
            throw new ConflictException("file.create.failed.message");
        }
    }

    /**
     * Checks that the project can be modified and user has WRITE permission.
     */
    private void requireModifiable(RulesProject project) {
        if (!projectStateValidator.canModify(project)) {
            throw new ConflictException("project.status.update.failed.message");
        }
        requirePermission(project, BasePermission.WRITE);
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
    private AProjectResource findFileArtefact(RulesProject project, String path) {
        validateResourcePath(path);
        AProjectArtefact found = findArtefactByPath(convertToFolder(project), path);
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
    private AProjectArtefact findExistingArtefact(RulesProject project, String path) {
        validateResourcePath(path);
        AProjectArtefact found = findArtefactByPath(convertToFolder(project), path);
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
     * @param project            the project root
     * @param fullPath           full resource path including file name
     * @param createMissing      if {@code true}, create missing intermediate folders;
     *                           if {@code false}, throw {@link NotFoundException}
     * @param conflictMessageKey error message key when a path segment is not a folder
     * @return the parent folder where the resource should be placed
     */
    private AProjectFolder resolveOrCreateFolders(RulesProject project,
                                                  String fullPath,
                                                  boolean createMissing,
                                                  String conflictMessageKey) throws ProjectException {
        String[] segments = fullPath.split("/");
        AProjectFolder targetFolder = project;
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

    private static String getFileName(String path) {
        int lastSlash = path.lastIndexOf('/');
        return lastSlash >= 0 ? path.substring(lastSlash + 1) : path;
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
            if (lcName.endsWith(".xls") && !lcName.endsWith(".xlsx") && !lcName.endsWith(".xlsm")) {
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
                mapped = fr.withChildren(childChildren.stream()
                        .sorted(FileNodeMapper.NODE_COMPARATOR)
                        .toList());
            }
            out.add(mapped);
        }
        return out;
    }


    private AProjectFolder convertToFolder(RulesProject project) {
        AProjectFolder filteredFolder = new AProjectFolder(new HashMap<>(),
                project.getProject(),
                project.getRepository(),
                project.getFolderPath());
        project.getArtefacts().forEach(filteredFolder::addArtefact);
        return filteredFolder;
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
