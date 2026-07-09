package org.openl.studio.projects.service;

import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.ParametersAreNonnullByDefault;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.acls.domain.BasePermission;

import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.ProjectStatus;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.project.abstraction.UserWorkspaceProject;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.Pageable;
import org.openl.security.acl.repository.RepositoryAclService;
import org.openl.studio.common.model.Capabilities;
import org.openl.studio.common.utils.AuditFields;
import org.openl.studio.common.utils.DateTimes;
import org.openl.studio.projects.model.FacetCount;
import org.openl.studio.projects.model.ProjectCapabilities;
import org.openl.studio.projects.model.ProjectLockInfo;
import org.openl.studio.projects.model.ProjectStatusSummary;
import org.openl.studio.projects.model.ProjectViewModel;
import org.openl.studio.projects.model.ProjectsPageResponse;
import org.openl.studio.projects.model.TagFacetSummary;
import org.openl.studio.projects.model.project.status.ProjectStatusViewModel;

/**
 * Abstract project service.
 *
 * @author Vladyslav Pikus
 */
@ParametersAreNonnullByDefault
@RequiredArgsConstructor
@Slf4j
public abstract class AbstractProjectService<T extends AProject> implements ProjectService<T> {

    protected static final Comparator<AProject> PROJECT_BUSINESS_NAME_ORDER =
            Comparator.comparing(AProject::getBusinessName, String.CASE_INSENSITIVE_ORDER);
    protected static final Predicate<AProject> ALL_PROJECTS = project -> true;
    private static final ProjectCapabilities EMPTY_CAPABILITIES = ProjectCapabilities.builder()
            .project(Capabilities.builder().build())
            .build();

    protected final RepositoryAclService designRepositoryAclService;
    protected final ProjectIdentifierMapper projectIdentifierMapper;
    protected final ProjectAccessService projectAccessService;

    @Override
    @Nonnull
    public ProjectsPageResponse getProjects(ProjectCriteriaQuery query, Pageable page) {
        var scope = projectScope(query, true);
        var summaryScope = query.includeSummary()
                ? projectScope(query.withoutFacetFilters(), false)
                : List.<T>of();

        // getStatus() is uncached, so resolve each project's status at most once and reuse it for both the
        // summary counts and the status sort instead of recomputing it for every sort comparison.
        Map<AProject, ProjectStatus> statuses;
        if (statusesNeeded(query)) {
            statuses = new IdentityHashMap<>();
            resolveStatuses(summaryScope, statuses);
            resolveStatuses(scope, statuses);
        } else {
            statuses = Map.of();
        }

        // Opt-in: only clients that ask for a summary get the counts, so the default list response and its
        // golden files stay unchanged. Counts use the search/dependency/ACL scope but intentionally ignore
        // selected facet values, preserving a stable multi-select rail while the page loads only one slice.
        var statusCounts = query.includeSummary()
                ? ProjectStatusSummary.of(summaryScope.stream()
                        .map(statuses::get)
                        .filter(Objects::nonNull)
                        .collect(Collectors.groupingBy(Function.identity(), Collectors.counting())))
                : null;
        var repositoryCounts = query.includeSummary() ? repositoryCounts(summaryScope, statuses) : null;
        var tagCounts = query.includeSummary() ? tagCounts(summaryScope) : null;

        var filtered = scope.stream()
                .filter(buildStatusFilterCriteria(query, statuses))
                .toList();
        var modifiedAt = modifiedAtNeeded(query) ? resolveModifiedAt(filtered) : Map.<AProject, Date>of();

        var matched = filtered.stream()
                .sorted(comparatorFor(query.sort(), statuses, modifiedAt))
                .toList();

        var pageProjects = matched.stream()
                .skip(page.getOffset())
                .limit(page.getPageSize())
                .toList();

        var content = pageProjects.stream()
                .map(project -> mapProjectResponse(project, query, statuses))
                .map(ProjectViewModel.Builder::build)
                .toList();

        var projectStatuses = query.includeStatus() ? projectStatuses(pageProjects) : null;

        return ProjectsPageResponse.of(
                content,
                page,
                matched.size(),
                statusCounts,
                repositoryCounts,
                tagCounts,
                projectStatuses);
    }

    private List<T> projectScope(ProjectCriteriaQuery query, boolean includeTagFilters) {
        var scopeFilter = buildFilterCriteria(query)
                .and(proj -> designRepositoryAclService.isGranted(proj, List.of(BasePermission.READ)));
        if (includeTagFilters) {
            scopeFilter = scopeFilter.and(buildTagsFilterCriteria(query));
        }
        return getProjects0(query)
                .filter(scopeFilter)
                .toList();
    }

    /** The project status used for the summary counts, if this service exposes one. */
    protected Optional<ProjectStatus> statusOf(AProject project) {
        return Optional.empty();
    }

    /** Current compilation status for projects in the returned page, if this service exposes it. */
    protected List<ProjectStatusViewModel> projectStatuses(List<? extends AProject> pageProjects) {
        return List.of();
    }

    /**
     * Restricts the page content to the requested status, or excludes deleted projects when no status is
     * requested. Applied to the page content only, not to the summary counts.
     */
    @Nonnull
    protected Predicate<AProject> buildStatusFilterCriteria(ProjectCriteriaQuery query,
                                                            Map<AProject, ProjectStatus> statuses) {
        if (query.includeDeleted()) {
            return ALL_PROJECTS;
        }
        return project -> !project.isDeleted();
    }

    private static boolean statusesNeeded(ProjectCriteriaQuery query) {
        return query.includeSummary() || query.hasStatusFilter() || "status".equals(query.sort());
    }

    private static boolean modifiedAtNeeded(ProjectCriteriaQuery query) {
        return "updated".equals(query.sort());
    }

    private void resolveStatuses(List<? extends AProject> scope, Map<AProject, ProjectStatus> statuses) {
        scope.stream()
                .filter(project -> !statuses.containsKey(project))
                .forEach(project -> statusOf(project).ifPresent(status -> statuses.put(project, status)));
    }

    private Map<AProject, Date> resolveModifiedAt(List<? extends AProject> projects) {
        Map<AProject, Date> modifiedAt = new IdentityHashMap<>();
        projects.forEach(project -> modifiedAt.put(project, modifiedAtOf(project)));
        return modifiedAt;
    }

    private Comparator<AProject> comparatorFor(@Nullable String sort,
                                               Map<AProject, ProjectStatus> statuses,
                                               Map<AProject, Date> modifiedAt) {
        return switch (sort == null ? "" : sort) {
            case "status" -> Comparator.<AProject, String>comparing(p -> statusSortKey(p, statuses),
                    String.CASE_INSENSITIVE_ORDER).thenComparing(PROJECT_BUSINESS_NAME_ORDER);
            case "updated" -> Comparator.<AProject, Date>comparing(modifiedAt::get,
                    Comparator.nullsLast(Comparator.reverseOrder())).thenComparing(PROJECT_BUSINESS_NAME_ORDER);
            default -> PROJECT_BUSINESS_NAME_ORDER;
        };
    }

    private static String statusSortKey(AProject project, Map<AProject, ProjectStatus> statuses) {
        var status = statuses.get(project);
        return status == null ? "" : status.name();
    }

    private static Date modifiedAtOf(AProject project) {
        try {
            var fileData = project.getFileData();
            return fileData == null ? null : fileData.getModifiedAt();
        } catch (RuntimeException e) {
            // getFileData() can rethrow a repository I/O failure as an unchecked exception. Treat an
            // unreadable project as having no timestamp rather than failing the whole listing.
            return null;
        }
    }

    @Nonnull
    protected Predicate<AProject> buildFilterCriteria(ProjectCriteriaQuery query) {
        Predicate<AProject> filter = ALL_PROJECTS;
        if (query.name() != null && !query.name().isBlank()) {
            var nameLower = query.name().toLowerCase();
            filter = filter.and(project -> project.getBusinessName().toLowerCase().contains(nameLower));
        }
        return filter;
    }

    @Nonnull
    private Predicate<AProject> buildTagsFilterCriteria(ProjectCriteriaQuery query) {
        Predicate<AProject> filter = ALL_PROJECTS;
        if (!query.tagValues().isEmpty()) {
            filter = project ->
                    project instanceof RulesProject rp
                            && projectHasTags(rp, query.tagValues());
        }
        return filter;
    }

    public boolean projectHasTags(RulesProject project, Map<String, Set<String>> tags) {
        var projectTags = project.getLocalTags();
        return tags.entrySet().stream().allMatch(tagValues -> {
            var value = projectTags.get(tagValues.getKey());
            return value != null && tagValues.getValue().contains(value);
        });
    }

    private List<FacetCount> repositoryCounts(List<? extends AProject> scope,
                                              Map<AProject, ProjectStatus> statuses) {
        var counts = new HashMap<String, Long>();
        var names = new HashMap<String, String>();
        for (var project : scope) {
            var id = repositoryFacetId(project, statuses);
            counts.merge(id, 1L, Long::sum);
            names.putIfAbsent(id, repositoryFacetName(project, id));
        }
        return counts.entrySet().stream()
                .map(entry -> new FacetCount(entry.getKey(), names.get(entry.getKey()), entry.getValue()))
                .sorted(Comparator.comparing(FacetCount::name, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(FacetCount::id, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private static String repositoryFacetId(AProject project, Map<AProject, ProjectStatus> statuses) {
        if (statuses.get(project) == ProjectStatus.LOCAL) {
            return ProjectCriteriaQuery.LOCAL_REPOSITORY_ID;
        }
        return project.getRepository().getId();
    }

    private static String repositoryFacetName(AProject project, String repositoryId) {
        if (ProjectCriteriaQuery.LOCAL_REPOSITORY_ID.equals(repositoryId)) {
            return "Local";
        }
        return Optional.ofNullable(project.getRepository().getName()).orElse(repositoryId);
    }

    private List<TagFacetSummary> tagCounts(List<? extends AProject> scope) {
        Map<String, Map<String, Long>> counts = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        scope.stream()
                .filter(RulesProject.class::isInstance)
                .map(RulesProject.class::cast)
                .map(RulesProject::getLocalTags)
                .filter(Objects::nonNull)
                .forEach(tags -> tags.forEach((type, value) -> addTagCount(counts, type, value)));
        return counts.entrySet().stream()
                .map(entry -> new TagFacetSummary(entry.getKey(), tagValueCounts(entry.getValue())))
                .toList();
    }

    private static void addTagCount(Map<String, Map<String, Long>> counts, String type, String value) {
        if (type != null && !type.isBlank() && value != null && !value.isBlank()) {
            counts.computeIfAbsent(type, ignored -> new TreeMap<>(String.CASE_INSENSITIVE_ORDER))
                    .merge(value, 1L, Long::sum);
        }
    }

    private static List<FacetCount> tagValueCounts(Map<String, Long> counts) {
        return counts.entrySet().stream()
                .map(entry -> new FacetCount(entry.getKey(), entry.getKey(), entry.getValue()))
                .toList();
    }

    protected abstract Stream<T> getProjects0(ProjectCriteriaQuery query);

    protected ProjectViewModel.Builder mapProjectResponse(T src,
                                                          ProjectCriteriaQuery query,
                                                          Map<AProject, ProjectStatus> statuses) {
        return mapProjectResponse(src, statuses);
    }

    protected ProjectViewModel.Builder mapProjectResponse(T src, Map<AProject, ProjectStatus> statuses) {
        var repository = src.getRepository();
        var builder = ProjectViewModel.builder()
                .name(src.getBusinessName())
                .id(projectIdentifierMapper.map(src))
                .repository(repository.getId());
        var fileData = fileDataOf(src);
        fileData.ifPresent(metadata -> {
            AuditFields.apply(metadata, builder::modifiedBy, builder::modifiedAt, builder::revision);
            Optional.ofNullable(metadata.getComment()).ifPresent(builder::comment);
        });
        applyLockInfo(src, builder);
        var designRepository = repository;
        if (src instanceof UserWorkspaceProject workspaceProject) {
            designRepository = workspaceProject.getDesignRepository();
            builder.status(workspaceStatus(workspaceProject, statuses)).branch(workspaceProject.getBranch());
        } else {
            var features = repository.supports();
            if (features.branches()) {
                fileData.map(FileData::getBranch).ifPresent(builder::branch);
            }
        }

        if (designRepository != null && designRepository.supports().mappedFolders()) {
            var path = src.getRealPath().replace('\\', '/');
            builder.path(path);
        }

        if (src instanceof RulesProject rulesProject) {
            var tags = rulesProject.getLocalTags();
            if (tags != null) {
                tags.forEach(builder::addTag);
            }
        }

        builder.capabilities(computeCapabilities(src));

        return builder;
    }

    private static ProjectStatus workspaceStatus(UserWorkspaceProject project, Map<AProject, ProjectStatus> statuses) {
        var status = statuses.get(project);
        return status == null ? project.getStatus() : status;
    }

    private Optional<FileData> fileDataOf(T project) {
        try {
            return Optional.ofNullable(project.getFileData());
        } catch (RuntimeException e) {
            log.warn("Failed to read metadata for project '{}' in repository '{}'. Omitting audit fields.",
                    safeProjectName(project), safeRepositoryId(project), e);
            return Optional.empty();
        }
    }

    private void applyLockInfo(T project, ProjectViewModel.Builder builder) {
        try {
            if (!project.isOpenedForEditing()) {
                var lockInfo = project.getLockInfo();
                if (lockInfo.isLocked()) {
                    builder.lockInfo(ProjectLockInfo.builder()
                            .lockedBy(lockInfo.getLockedBy())
                            .lockedAt(DateTimes.atSystemZone(lockInfo.getLockedAt()))
                            .build());
                }
            }
        } catch (RuntimeException e) {
            log.warn("Failed to read lock info for project '{}' in repository '{}'. Omitting lock fields.",
                    safeProjectName(project), safeRepositoryId(project), e);
        }
    }

    private ProjectCapabilities computeCapabilities(T project) {
        try {
            return projectAccessService.computeCapabilities(project);
        } catch (RuntimeException e) {
            log.warn("Failed to compute capabilities for project '{}' in repository '{}'. Returning empty capabilities.",
                    safeProjectName(project), safeRepositoryId(project), e);
            return EMPTY_CAPABILITIES;
        }
    }

    private static String safeProjectName(AProject project) {
        try {
            return project.getBusinessName();
        } catch (RuntimeException e) {
            return "<unknown>";
        }
    }

    private static String safeRepositoryId(AProject project) {
        try {
            var repository = project.getRepository();
            return repository == null ? "<none>" : repository.getId();
        } catch (RuntimeException e) {
            return "<unknown>";
        }
    }

}
