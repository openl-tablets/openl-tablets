package org.openl.studio.projects.service.resources;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.Deque;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.function.Predicate;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

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
import org.openl.studio.common.validation.BeanValidationProvider;
import org.openl.studio.projects.model.resources.FileResource;
import org.openl.studio.projects.model.resources.FolderResource;
import org.openl.studio.projects.model.resources.Resource;
import org.openl.studio.projects.validator.ProjectStateValidator;
import org.openl.studio.projects.validator.resource.ResourceCriteriaQueryValidator;
import org.openl.util.FileSignatureHelper;
import org.openl.util.FileTypeHelper;
import org.openl.util.FileUtils;
import org.openl.util.StringUtils;

/**
 * Implementation of {@link ProjectResourcesService}.
 * Uses iterative queue-based traversal instead of recursion to avoid stack overflow.
 */
@Service
@Validated
public class ProjectResourcesServiceImpl implements ProjectResourcesService {

    private static final Comparator<Resource> RESOURCE_COMPARATOR = Comparator
            .comparing((Resource r) -> r instanceof FileResource)
            .thenComparing(r -> r.name, String.CASE_INSENSITIVE_ORDER);

    private final AclProjectsHelper aclProjectsHelper;
    private final BeanValidationProvider validationProvider;
    private final ResourceCriteriaQueryValidator queryValidator;
    private final ProjectStateValidator projectStateValidator;

    public ProjectResourcesServiceImpl(AclProjectsHelper aclProjectsHelper,
                                       BeanValidationProvider validationProvider,
                                       ResourceCriteriaQueryValidator queryValidator,
                                       ProjectStateValidator projectStateValidator) {
        this.aclProjectsHelper = aclProjectsHelper;
        this.validationProvider = validationProvider;
        this.queryValidator = queryValidator;
        this.projectStateValidator = projectStateValidator;
    }

    @Override
    public List<Resource> getResources(@NotNull RulesProject project,
                                       @NotNull ResourceCriteriaQuery query,
                                       boolean recursive,
                                       @NotNull ResourceViewMode viewMode) {
        // Verify user has READ permission on the project
        if (!aclProjectsHelper.hasPermission(project, BasePermission.READ)) {
            throw new ForbiddenException("default.message");
        }
        validationProvider.validate(query, queryValidator);
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
    public AProjectResource getResource(@NotNull RulesProject project, @NotBlank String resourceId) {
        if (!aclProjectsHelper.hasPermission(project, BasePermission.READ)) {
            throw new ForbiddenException("default.message");
        }
        AProjectFolder projectFolder = convertToFolder(project);
        AProjectArtefact found = findArtefactById(projectFolder, resourceId);
        if (found == null || found.isFolder()) {
            throw new NotFoundException("resource.not.found.message");
        }
        if (!aclProjectsHelper.hasPermission(found, BasePermission.READ)) {
            throw new ForbiddenException("default.message");
        }
        return (AProjectResource) found;
    }

    @Override
    public void updateResource(@NotNull RulesProject project,
                               @NotBlank String resourceId,
                               @NotNull InputStream content) {
        if (!projectStateValidator.canModify(project)) {
            throw new ConflictException("project.status.update.failed.message");
        }
        if (!aclProjectsHelper.hasPermission(project, BasePermission.WRITE)) {
            throw new ForbiddenException("default.message");
        }
        AProjectFolder projectFolder = convertToFolder(project);
        AProjectArtefact found = findArtefactById(projectFolder, resourceId);
        if (found == null || found.isFolder()) {
            throw new NotFoundException("resource.not.found.message");
        }
        if (!aclProjectsHelper.hasPermission(found, BasePermission.WRITE)) {
            throw new ForbiddenException("default.message");
        }
        var resource = (AProjectResource) found;
        InputStream validatedContent = validateContent(resource.getName(), content);
        try {
            resource.setContent(validatedContent);
        } catch (ProjectException e) {
            throw new ConflictException("resource.update.failed.message");
        }
    }

    @Override
    public void deleteResource(@NotNull RulesProject project, @NotBlank String resourceId) {
        if (!projectStateValidator.canModify(project)) {
            throw new ConflictException("project.status.update.failed.message");
        }
        AProjectFolder projectFolder = convertToFolder(project);
        AProjectArtefact found = findArtefactById(projectFolder, resourceId);
        if (found == null) {
            throw new NotFoundException("resource.not.found.message");
        }
        if (!aclProjectsHelper.hasPermission(found, BasePermission.DELETE)) {
            throw new ForbiddenException("default.message");
        }
        try {
            found.delete();
        } catch (ProjectException e) {
            throw new ConflictException("resource.delete.failed.message");
        }
    }

    @Override
    public void copyResource(@NotNull RulesProject project,
                             @NotBlank String resourceId,
                             @NotBlank String destinationPath) {
        if (!projectStateValidator.canModify(project)) {
            throw new ConflictException("project.status.update.failed.message");
        }
        if (!aclProjectsHelper.hasPermission(project, BasePermission.WRITE)) {
            throw new ForbiddenException("default.message");
        }
        AProjectFolder projectFolder = convertToFolder(project);
        AProjectArtefact found = findArtefactById(projectFolder, resourceId);
        if (found == null || found.isFolder()) {
            throw new NotFoundException("resource.not.found.message");
        }
        if (!aclProjectsHelper.hasPermission(found, BasePermission.READ)) {
            throw new ForbiddenException("default.message");
        }

        String[] segments = destinationPath.split("/");
        AProjectFolder targetFolder = project;
        try {
            // Resolve or create intermediate folders
            for (int i = 0; i < segments.length - 1; i++) {
                String segment = segments[i];
                if (!targetFolder.hasArtefact(segment)) {
                    targetFolder = targetFolder.addFolder(segment);
                } else {
                    AProjectArtefact artefact = targetFolder.getArtefact(segment);
                    if (!artefact.isFolder()) {
                        throw new ConflictException("resource.copy.path.conflict.message",
                                artefact.getInternalPath());
                    }
                    targetFolder = (AProjectFolder) artefact;
                }
            }
            if (!aclProjectsHelper.hasPermission(targetFolder, BasePermission.CREATE)) {
                throw new ForbiddenException("default.message");
            }
            String fileName = segments[segments.length - 1];
            try (var content = ((AProjectResource) found).getContent()) {
                targetFolder.addResource(fileName, content);
            }
        } catch (ProjectException | IOException e) {
            throw new ConflictException("resource.copy.failed.message");
        }
    }

    @Override
    public void createResource(@NotNull RulesProject project,
                               @NotBlank String path,
                               @NotNull InputStream content,
                               boolean createFolders) {
        if (!projectStateValidator.canModify(project)) {
            throw new ConflictException("project.status.update.failed.message");
        }
        if (!aclProjectsHelper.hasPermission(project, BasePermission.WRITE)) {
            throw new ForbiddenException("default.message");
        }
        try {
            NameChecker.validatePath(path);
        } catch (IOException e) {
            throw new BadRequestException("resource.path.invalid.message");
        }

        String[] segments = path.split("/");
        AProjectFolder targetFolder = project;
        int firstMissing = segments.length - 1; // index of first segment to create
        try {
            // Resolve existing folders
            for (int i = 0; i < segments.length - 1; i++) {
                String segment = segments[i];
                if (!targetFolder.hasArtefact(segment)) {
                    if (!createFolders) {
                        throw new NotFoundException("resource.parent.not.found.message",
                                segment);
                    }
                    firstMissing = i;
                    break;
                }
                AProjectArtefact artefact = targetFolder.getArtefact(segment);
                if (!artefact.isFolder()) {
                    throw new ConflictException("resource.path.not.folder.message",
                            artefact.getInternalPath());
                }
                targetFolder = (AProjectFolder) artefact;
            }
            // Check permission on the deepest existing folder before creating anything
            if (!aclProjectsHelper.hasPermission(targetFolder, BasePermission.CREATE)) {
                throw new ForbiddenException("default.message");
            }
            // Create missing intermediate folders
            for (int i = firstMissing; i < segments.length - 1; i++) {
                targetFolder = targetFolder.addFolder(segments[i]);
            }
            String fileName = segments[segments.length - 1];
            InputStream validatedContent = validateContent(fileName, content);
            targetFolder.addResource(fileName, validatedContent);
        } catch (ProjectException e) {
            throw new ConflictException("resource.create.failed.message");
        }
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

    private AProjectArtefact findArtefactById(AProjectFolder rootFolder, String resourceId) {
        Deque<AProjectFolder> queue = new ArrayDeque<>();
        queue.add(rootFolder);
        while (!queue.isEmpty()) {
            AProjectFolder folder = queue.poll();
            for (AProjectArtefact artefact : folder.getArtefacts()) {
                if (artefact.getId().equals(resourceId)) {
                    return artefact;
                }
                if (artefact.isFolder()) {
                    queue.add((AProjectFolder) artefact);
                }
            }
        }
        return null;
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
        } catch (ProjectException ignored) {
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
            var extensions = query.extensions();
            filter = filter.and(artefact -> {
                if (artefact.isFolder()) {
                    return true; // Folders always pass extension filter
                }
                var ext = FileUtils.getExtension(artefact.getName());
                return ext != null && extensions.stream()
                        .anyMatch(queryExt -> queryExt.equalsIgnoreCase(ext));
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
        if (artefact.isFolder()) {
            return mapResource(artefact, FolderResource.builder()).build();
        } else {
            var builder = mapResource(artefact, FileResource.builder())
                    .extension(FileUtils.getExtension(artefact.getName()));

            FileData fileData = artefact.getFileData();
            if (fileData != null) {
                builder.size(fileData.getSize());
                builder.lastModified(toZonedDateTime(fileData.getModifiedAt()));
            }

            return builder.build();
        }
    }

    private <T extends Resource.Builder<T>> T mapResource(AProjectArtefact artefact, T builder) {
        String path = artefact.getInternalPath();
        String name = artefact.getName();
        builder.id(artefact.getId())
                .name(name)
                .basePath(getParentPath(path));
        return builder;
    }

    /**
     * Converts Date to ZonedDateTime safely.
     */
    private ZonedDateTime toZonedDateTime(Date date) {
        if (date == null) {
            return null;
        }
        return date.toInstant().atZone(ZoneId.systemDefault());
    }

}
