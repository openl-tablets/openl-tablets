package org.openl.studio.projects.service.resources;

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

        if (viewMode == ResourceViewMode.NESTED && recursive) {
            return buildNested(baseFolder, filter);
        } else {
            return buildFlatList(baseFolder, filter, recursive);
        }
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
