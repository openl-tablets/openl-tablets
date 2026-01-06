package org.openl.studio.projects.service.resources;

import java.nio.file.Path;
import java.nio.file.Paths;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import org.openl.rules.common.ProjectException;
import org.openl.rules.common.impl.ArtefactPathImpl;
import org.openl.rules.project.abstraction.AProjectArtefact;
import org.openl.rules.project.abstraction.AProjectFolder;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.rest.acl.service.AclProjectsHelper;
import org.openl.studio.common.exception.BadRequestException;
import org.openl.studio.common.exception.NotFoundException;
import org.openl.studio.projects.model.resources.FileResource;
import org.openl.studio.projects.model.resources.FolderResource;
import org.openl.studio.projects.model.resources.Resource;
import org.openl.util.FileUtils;
import org.openl.util.StringUtils;

/**
 * Implementation of {@link ProjectResourcesService}.
 * Uses iterative queue-based traversal instead of recursion to avoid stack overflow.
 */
@Service
public class ProjectResourcesServiceImpl implements ProjectResourcesService {

    private static final Comparator<Resource> RESOURCE_COMPARATOR = Comparator
            .comparing((Resource r) -> r instanceof FileResource)
            .thenComparing(r -> r.name, String.CASE_INSENSITIVE_ORDER);

    private final AclProjectsHelper aclProjectsHelper;

    public ProjectResourcesServiceImpl(AclProjectsHelper aclProjectsHelper) {
        this.aclProjectsHelper = aclProjectsHelper;
    }

    @Override
    public List<Resource> getResources(RulesProject project,
                                        ResourceCriteriaQuery query,
                                        boolean recursive,
                                        ResourceViewMode viewMode) {
        validateQuery(query);

        try {
            AProjectFolder baseFolder = resolveBaseFolder(project, query);

            if (viewMode == ResourceViewMode.NESTED) {
                return buildNestedStructure(baseFolder, query, recursive);
            } else {
                return buildFlatList(baseFolder, query, recursive);
            }
        } catch (ProjectException e) {
            throw new NotFoundException("resource.not.found.message", e);
        }
    }

    /**
     * Validates the query parameters for security and correctness.
     */
    private void validateQuery(ResourceCriteriaQuery query) {
        if (query == null) {
            return;
        }

        // Validate namePattern
        if (query.namePattern() != null) {
            if (query.namePattern().length() > 255) {
                throw new BadRequestException("name.pattern.too.long");
            }
            if (query.namePattern().contains("/") || query.namePattern().contains("\\")) {
                throw new BadRequestException("invalid.name.pattern");
            }
        }

        // Validate extensions
        if (query.extensions() != null) {
            for (String ext : query.extensions()) {
                if (ext == null || ext.length() > 20 || !ext.matches("^[a-zA-Z0-9]+$")) {
                    throw new BadRequestException("invalid.extension", new Object[]{ext});
                }
            }
        }
    }

    private AProjectFolder resolveBaseFolder(RulesProject project, ResourceCriteriaQuery query) throws ProjectException {
        if (query == null || StringUtils.isBlank(query.basePath())) {
            return project;
        }

        String basePath = query.basePath();
        validateBasePath(basePath);

        AProjectArtefact artefact = project.getArtefactByPath(new ArtefactPathImpl(basePath));

        if (artefact == null || !artefact.isFolder()) {
            throw new BadRequestException("base.path.not.folder", new Object[]{basePath});
        }

        return (AProjectFolder) artefact;
    }

    /**
     * Validates base path for path traversal attacks.
     */
    private void validateBasePath(String basePath) {
        if (basePath == null || basePath.isEmpty()) {
            return;
        }

        // Quick check for obvious attacks
        if (basePath.startsWith("/") || basePath.startsWith("\\")) {
            throw new BadRequestException("invalid.base.path", new Object[]{basePath});
        }

        try {
            // Normalize and validate
            Path normalized = Paths.get(basePath).normalize();
            String normalizedStr = normalized.toString();

            // Check for path traversal after normalization
            if (normalizedStr.startsWith("..") ||
                    normalizedStr.contains("/../") ||
                    normalizedStr.contains("\\..\\") ||
                    normalizedStr.contains("\\") ||
                    normalized.isAbsolute()) {
                throw new BadRequestException("invalid.base.path", new Object[]{basePath});
            }
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("invalid.base.path", new Object[]{basePath});
        }
    }

    /**
     * Builds a flat list of resources using iterative queue-based traversal.
     */
    private List<Resource> buildFlatList(AProjectFolder rootFolder,
                                          ResourceCriteriaQuery query,
                                          boolean recursive) throws ProjectException {
        List<Resource> result = new ArrayList<>();

        Deque<AProjectFolder> queue = new ArrayDeque<>();
        queue.add(rootFolder);

        while (!queue.isEmpty()) {
            AProjectFolder folder = queue.poll();

            for (AProjectArtefact artefact : folder.getArtefacts()) {
                Resource resource = mapArtefact(artefact);

                if (applyFilter(resource, query)) {
                    result.add(resource);
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
                                                 ResourceCriteriaQuery query,
                                                 boolean recursive) throws ProjectException {
        if (!recursive) {
            return buildDirectChildren(rootFolder, query);
        }

        // First pass: collect all resources
        Map<String, ResourceNode> nodesByPath = new HashMap<>();
        Deque<AProjectFolder> queue = new ArrayDeque<>();
        String rootPath = rootFolder instanceof RulesProject ? "" : rootFolder.getInternalPath();
        queue.add(rootFolder);

        while (!queue.isEmpty()) {
            AProjectFolder folder = queue.poll();

            for (AProjectArtefact artefact : folder.getArtefacts()) {
                String path = artefact.getInternalPath();
                Resource resource = mapArtefact(artefact);

                if (applyFilter(resource, query)) {
                    nodesByPath.put(path, new ResourceNode(resource, artefact.isFolder()));

                    if (artefact.isFolder()) {
                        queue.add((AProjectFolder) artefact);
                    }
                }
            }
        }

        // Build parent-to-children mapping - O(n)
        Map<String, List<Resource>> childrenByParent = new HashMap<>();
        for (Map.Entry<String, ResourceNode> entry : nodesByPath.entrySet()) {
            String path = entry.getKey();
            String parentPath = getParentPath(path);
            childrenByParent.computeIfAbsent(parentPath, k -> new ArrayList<>())
                    .add(entry.getValue().resource);
        }

        // Second pass: build tree from leaves up using sorted paths (longest first) - O(n log n)
        List<String> sortedPaths = new ArrayList<>(nodesByPath.keySet());
        sortedPaths.sort((a, b) -> Integer.compare(b.length(), a.length()));

        for (String path : sortedPaths) {
            ResourceNode node = nodesByPath.get(path);
            if (node.isFolder) {
                List<Resource> children = childrenByParent.getOrDefault(path, List.of());
                if (!children.isEmpty()) {
                    List<Resource> sortedChildren = new ArrayList<>(children);
                    sortedChildren.sort(RESOURCE_COMPARATOR);
                    node.resource = ((FolderResource) node.resource).withChildren(sortedChildren);
                }
            }
        }

        // Collect root-level children
        List<Resource> rootChildren = childrenByParent.getOrDefault(rootPath, List.of());
        List<Resource> result = new ArrayList<>(rootChildren);
        result.sort(RESOURCE_COMPARATOR);
        return result;
    }

    /**
     * Builds direct children only (non-recursive nested mode).
     */
    private List<Resource> buildDirectChildren(AProjectFolder folder,
                                                ResourceCriteriaQuery query) {
        List<Resource> children = new ArrayList<>();

        for (AProjectArtefact artefact : folder.getArtefacts()) {
            Resource resource = mapArtefact(artefact);
            if (applyFilter(resource, query)) {
                children.add(resource);
            }
        }

        children.sort(RESOURCE_COMPARATOR);
        return children;
    }

    /**
     * Extracts parent path from a full path.
     */
    private String getParentPath(String path) {
        if (path == null || path.isEmpty()) {
            return null;
        }
        int lastSlash = path.lastIndexOf('/');
        return lastSlash > 0 ? path.substring(0, lastSlash) : null;
    }

    /**
     * Maps an artefact to a Resource DTO.
     * The basePath is calculated from the artefact's internal path.
     */
    private Resource mapArtefact(AProjectArtefact artefact) {
        if (artefact.isFolder()) {
            var builder =  FolderResource.builder();
            mapResource(artefact, builder);
            return builder.build();
        } else {
            var builder =  FileResource.builder();
            mapResource(artefact, builder);
            builder.extension(FileUtils.getExtension(artefact.getName()));

            FileData fileData = artefact.getFileData();
            if (fileData != null) {
                builder.size(fileData.getSize());
                builder.lastModified(toZonedDateTime(fileData.getModifiedAt()));
            }

            return builder.build();
        }
    }

    private void mapResource(AProjectArtefact artefact, Resource.Builder<?> builder) {
        String path = artefact.getInternalPath();
        String name = artefact.getName();
        builder.id(artefact.getId())
                .name(name)
                .basePath(getParentPath(path));
    }

    /**
     * Applies filter criteria to a resource.
     * Folders are always included when extension filter is applied to preserve tree structure.
     */
    private boolean applyFilter(Resource resource, ResourceCriteriaQuery query) {
        if (query == null) {
            return true;
        }

        // Name pattern filter (case-insensitive contains)
        if (StringUtils.isNotBlank(query.namePattern())) {
            if (!resource.name.toLowerCase().contains(query.namePattern().toLowerCase())) {
                return false;
            }
        }

        // Extension filter (only applies to files, folders always pass to preserve tree structure)
        if (query.extensions() != null && !query.extensions().isEmpty()) {
            if (resource instanceof FileResource fileResource) {
                // Extensions in query are already lowercase (normalized in builder)
                String ext = fileResource.extension;
                return ext != null && query.extensions().stream()
                        .anyMatch(queryExt -> queryExt.equalsIgnoreCase(ext));
            }
            // Folders always pass extension filter
        }

        return true;
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
