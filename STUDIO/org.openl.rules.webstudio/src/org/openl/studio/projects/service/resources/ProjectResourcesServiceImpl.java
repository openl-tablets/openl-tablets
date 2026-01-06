package org.openl.studio.projects.service.resources;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import jakarta.validation.constraints.NotNull;

import org.springframework.security.acls.domain.BasePermission;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import org.openl.rules.common.ProjectException;
import org.openl.rules.project.abstraction.AProjectArtefact;
import org.openl.rules.project.abstraction.AProjectFolder;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.rest.acl.service.AclProjectsHelper;
import org.openl.studio.common.exception.BadRequestException;
import org.openl.studio.common.exception.ForbiddenException;
import org.openl.studio.common.validation.BeanValidationProvider;
import org.openl.studio.projects.model.resources.FileResource;
import org.openl.studio.projects.model.resources.FolderResource;
import org.openl.studio.projects.model.resources.Resource;
import org.openl.studio.projects.validator.resource.ResourceCriteriaQueryValidator;
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

    public ProjectResourcesServiceImpl(AclProjectsHelper aclProjectsHelper,
                                       BeanValidationProvider validationProvider,
                                       ResourceCriteriaQueryValidator queryValidator) {
        this.aclProjectsHelper = aclProjectsHelper;
        this.validationProvider = validationProvider;
        this.queryValidator = queryValidator;
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
        if (viewMode == ResourceViewMode.NESTED) {
            return buildNestedStructure(baseFolder, filter, recursive);
        } else {
            return buildFlatList(baseFolder, filter, recursive);
        }
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
     * Builds a nested tree structure using iterative two-pass approach.
     * First pass: collect all resources in a flat map keyed by path.
     * Second pass: build the tree by linking children to parents using O(n) algorithm.
     */
    private List<Resource> buildNestedStructure(AProjectFolder rootFolder,
                                                Predicate<AProjectArtefact> filter,
                                                boolean recursive) {
        if (!recursive) {
            return rootFolder.getArtefacts().stream()
                    .filter(filter)
                    .map(this::mapArtefact)
                    .sorted(RESOURCE_COMPARATOR)
                    .toList();
        }

        // First pass: collect all resources
        Map<String, ResourceNode> nodesByPath = new HashMap<>();
        Deque<AProjectFolder> queue = new ArrayDeque<>();
        String rootPath = rootFolder.getInternalPath();
        queue.add(rootFolder);

        while (!queue.isEmpty()) {
            AProjectFolder folder = queue.poll();

            for (AProjectArtefact artefact : folder.getArtefacts()) {
                if (filter.test(artefact)) {
                    String path = artefact.getInternalPath();
                    nodesByPath.put(path, new ResourceNode(mapArtefact(artefact), artefact.isFolder()));

                    if (artefact.isFolder()) {
                        queue.add((AProjectFolder) artefact);
                    }
                }
            }
        }

        // Build parent-to-children mapping - O(n)
        Map<String, List<Resource>> childrenByParent = new HashMap<>();
        for (var entry : nodesByPath.entrySet()) {
            String parentPath = getParentPath(entry.getKey());
            childrenByParent.computeIfAbsent(parentPath, k -> new ArrayList<>())
                    .add(entry.getValue().resource);
        }

        // Second pass: build tree from leaves up using sorted paths (longest first) - O(n log n)
        nodesByPath.keySet().stream()
                .sorted((a, b) -> Integer.compare(b.length(), a.length()))
                .forEach(path -> {
                    ResourceNode node = nodesByPath.get(path);
                    if (node.isFolder) {
                        List<Resource> children = childrenByParent.getOrDefault(path, List.of());
                        if (!children.isEmpty()) {
                            var sortedChildren = children.stream()
                                    .sorted(RESOURCE_COMPARATOR)
                                    .toList();
                            node.resource = ((FolderResource) node.resource).withChildren(sortedChildren);
                        }
                    }
                });

        // Collect root-level children
        return childrenByParent.getOrDefault(rootPath, List.of()).stream()
                .sorted(RESOURCE_COMPARATOR)
                .toList();
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

    /**
     * Node for building the tree structure.
     */
    private static class ResourceNode {
        Resource resource;
        final boolean isFolder;

        ResourceNode(Resource resource, boolean isFolder) {
            this.resource = resource;
            this.isFolder = isFolder;
        }
    }
}
