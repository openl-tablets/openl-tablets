package org.openl.studio.projects.service.resources;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
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
import org.openl.rules.repository.api.FileData;
import org.openl.rules.rest.acl.service.AclProjectsHelper;
import org.openl.rules.webstudio.util.NameChecker;
import org.openl.studio.common.exception.BadRequestException;
import org.openl.studio.common.exception.ConflictException;
import org.openl.studio.common.exception.ForbiddenException;
import org.openl.studio.common.exception.NotFoundException;
import org.openl.studio.projects.model.resources.FileResource;
import org.openl.studio.projects.model.resources.FolderResource;
import org.openl.studio.projects.model.resources.Resource;
import org.openl.studio.projects.validator.ProjectStateValidator;
import org.openl.util.FileSignatureHelper;
import org.openl.util.FileTypeHelper;
import org.openl.util.FileUtils;
import org.openl.util.StringUtils;

/**
 * Implementation of {@link ProjectResourcesService}.
 * Uses iterative queue-based traversal instead of recursion to avoid stack overflow.
 */
@Slf4j
@RequiredArgsConstructor
@Service
@Validated
public class ProjectResourcesServiceImpl implements ProjectResourcesService {

    private static final Comparator<Resource> RESOURCE_COMPARATOR = Comparator
            .comparing((Resource r) -> r instanceof FileResource)
            .thenComparing(Resource::getName, String.CASE_INSENSITIVE_ORDER);

    private final AclProjectsHelper aclProjectsHelper;
    private final ProjectStateValidator projectStateValidator;

    @Override
    public List<Resource> getResources(@NotNull RulesProject project,
                                       @NotNull ResourceCriteriaQuery query,
                                       boolean recursive,
                                       @NotNull ResourceViewMode viewMode) {
        // Verify user has READ permission on the project
        if (!aclProjectsHelper.hasPermission(project, BasePermission.READ)) {
            throw new ForbiddenException("default.message");
        }
        AProjectFolder projectFolder = convertToFolder(project);
        AProjectFolder baseFolder = resolveBaseFolder(projectFolder, query);

        var filter = buildFilterCriteria(query);

        if (viewMode == ResourceViewMode.NESTED && recursive) {
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
            throw new ConflictException("resource.update.failed.message");
        }
    }

    @Override
    public void deleteResource(@NotNull RulesProject project, @NotBlank String path) {
        requireModifiable(project);
        validateResourcePath(path);
        AProjectArtefact found = findArtefactByPath(convertToFolder(project), path);
        if (found == null) {
            throw new NotFoundException("resource.not.found.message");
        }
        requirePermission(found, BasePermission.DELETE);
        try {
            found.delete();
        } catch (ProjectException e) {
            throw new ConflictException("resource.delete.failed.message");
        }
    }

    @Override
    public void copyResource(@NotNull RulesProject project,
                             @NotBlank String sourcePath,
                             @NotBlank String destinationPath) {
        requireModifiable(project);
        validateResourcePath(destinationPath);
        var source = findFileArtefact(project, sourcePath);
        requirePermission(source, BasePermission.READ);

        try {
            var targetFolder = resolveOrCreateFolders(project, destinationPath,
                    true, "resource.copy.path.conflict.message");
            String fileName = getFileName(destinationPath);
            try (var content = source.getContent()) {
                targetFolder.addResource(fileName, content);
            }
        } catch (ProjectException | IOException e) {
            throw new ConflictException("resource.copy.failed.message");
        }
    }

    @Override
    public void moveResource(@NotNull RulesProject project,
                             @NotBlank String sourcePath,
                             @NotBlank String destinationPath) {
        requireModifiable(project);
        validateResourcePath(destinationPath);
        var source = findFileArtefact(project, sourcePath);
        requirePermission(source, BasePermission.READ);
        requirePermission(source, BasePermission.DELETE);

        try {
            var targetFolder = resolveOrCreateFolders(project, destinationPath,
                    true, "resource.move.path.conflict.message");
            String fileName = getFileName(destinationPath);
            try (var content = source.getContent()) {
                targetFolder.addResource(fileName, content);
            }
            try {
                source.delete();
            } catch (ProjectException deleteEx) {
                // Rollback: remove the copied resource to avoid duplication
                try {
                    targetFolder.getArtefact(fileName).delete();
                } catch (ProjectException rollbackEx) {
                    log.error("Failed to rollback copied resource '{}' after move failure", destinationPath, rollbackEx);
                }
                throw deleteEx;
            }
        } catch (ProjectException | IOException e) {
            throw new ConflictException("resource.move.failed.message");
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
                    createFolders, "resource.path.not.folder.message");
            String fileName = getFileName(path);
            InputStream validatedContent = validateContent(fileName, content);
            targetFolder.addResource(fileName, validatedContent);
        } catch (ProjectException e) {
            throw new ConflictException("resource.create.failed.message");
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
            throw new NotFoundException("resource.not.found.message");
        }
        return (AProjectResource) found;
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
                    throw new NotFoundException("resource.parent.not.found.message", segment);
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
                    throw new BadRequestException("resource.content.invalid.message");
                }
            } else {
                if (!FileSignatureHelper.isArchiveSign(sign)) {
                    throw new BadRequestException("resource.content.invalid.message");
                }
            }
        } catch (IOException e) {
            throw new BadRequestException("resource.content.invalid.message");
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
            throw new BadRequestException("resource.path.invalid.message");
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

    private List<Resource> buildNested(AProjectFolder rootFolder,
                                       Predicate<AProjectArtefact> filter) {

        // folder -> built children list
        var builtChildren = new IdentityHashMap<AProjectFolder, List<Resource>>();
        // stack frames for post-order traversal
        record Frame(AProjectFolder folder, boolean expanded) {}

        Deque<Frame> stack = new ArrayDeque<>();
        stack.push(new Frame(rootFolder, false));

        while (!stack.isEmpty()) {
            var frame = stack.pop();
            var folder = frame.folder();

            if (!frame.expanded()) {
                // 1) push marker to process after children
                stack.push(new Frame(folder, true));

                // 2) push child folders to process first
                for (var artefact : folder.getArtefacts()) {
                    if (artefact.isFolder()) {
                        stack.push(new Frame((AProjectFolder) artefact, false));
                    }
                }
                continue;
            }

            // Children are already processed -> build this folder children list
            List<Resource> out = new ArrayList<>();

            for (var artefact : folder.getArtefacts()) {
                if (!artefact.isFolder()) {
                    if (filter.test(artefact)) {
                        out.add(mapArtefact(artefact));
                    }
                    continue;
                }

                var childFolder = (AProjectFolder) artefact;
                var childChildren = builtChildren.getOrDefault(childFolder, List.of());

                boolean includeFolder = filter.test(artefact) || !childChildren.isEmpty();
                if (!includeFolder) {
                    continue;
                }

                Resource mapped = mapArtefact(artefact);

                if (mapped instanceof FolderResource fr && !childChildren.isEmpty()) {
                    var sortedChildren = childChildren.stream()
                            .sorted(RESOURCE_COMPARATOR)
                            .toList();
                    mapped = fr.withChildren(sortedChildren);
                }

                out.add(mapped);
            }

            out.sort(RESOURCE_COMPARATOR);
            builtChildren.put(folder, out);
        }

        return builtChildren.getOrDefault(rootFolder, List.of());
    }


    private AProjectFolder convertToFolder(RulesProject project) {
        AProjectFolder filteredFolder = new AProjectFolder(new HashMap<>(),
                project.getProject(),
                project.getRepository(),
                project.getFolderPath());
        project.getArtefacts().forEach(filteredFolder::addArtefact);
        return filteredFolder;
    }

    private AProjectFolder resolveBaseFolder(AProjectFolder folder, ResourceCriteriaQuery query) {
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
            throw new BadRequestException("resource.base-path.not-folder.message", new Object[]{query.basePath()});
        }
        return (AProjectFolder) artefact;
    }

    /**
     * Builds a filter predicate based on the query criteria and ACL permissions.
     * The filter is applied before mapping to DTO to minimize overhead.
     */
    private Predicate<AProjectArtefact> buildFilterCriteria(ResourceCriteriaQuery query) {
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
    private List<Resource> buildFlatList(AProjectFolder rootFolder,
                                         Predicate<AProjectArtefact> filter,
                                         boolean recursive) {
        List<Resource> result = new ArrayList<>();

        Deque<AProjectFolder> queue = new ArrayDeque<>();
        queue.add(rootFolder);

        while (!queue.isEmpty()) {
            AProjectFolder folder = queue.poll();
            for (AProjectArtefact artefact : folder.getArtefacts()) {
                if (filter.test(artefact)) {
                    result.add(mapArtefact(artefact));
                }

                if (recursive && artefact.isFolder()) {
                    queue.add((AProjectFolder) artefact);
                }
            }
        }

        result.sort(RESOURCE_COMPARATOR);
        return result;
    }

    /**
     * Extracts parent path from a full path.
     * Returns empty string for root-level items (no slash in path).
     * Returns null only for null or empty input.
     */
    private String getParentPath(String path) {
        if (path == null || path.isEmpty()) {
            return null;
        }
        int lastSlash = path.lastIndexOf('/');
        if (lastSlash < 0) {
            // No slash means root level, parent is empty string (project root)
            return "";
        }
        return lastSlash == 0 ? "" : path.substring(0, lastSlash);
    }

    /**
     * Maps an artefact to a Resource DTO.
     * The basePath is calculated from the artefact's internal path.
     */
    private Resource mapArtefact(AProjectArtefact artefact) {
        String path = artefact.getInternalPath();
        String name = artefact.getName();
        String basePath = getParentPath(path);

        if (artefact.isFolder()) {
            return FolderResource.builder()
                    .path(path)
                    .name(name)
                    .basePath(basePath)
                    .build();
        } else {
            var builder = FileResource.builder()
                    .path(path)
                    .name(name)
                    .basePath(basePath)
                    .extension(FileUtils.getExtension(name));

            FileData fileData = artefact.getFileData();
            if (fileData != null) {
                builder.size(fileData.getSize());
                builder.lastModified(toZonedDateTime(fileData.getModifiedAt()));
            }

            return builder.build();
        }
    }

    /**
     * Converts Date to ZonedDateTime safely.
     */
    private ZonedDateTime toZonedDateTime(Date date) {
        if (date == null) {
            return null;
        }
        return date.toInstant().atZone(ZoneOffset.UTC);
    }

}
