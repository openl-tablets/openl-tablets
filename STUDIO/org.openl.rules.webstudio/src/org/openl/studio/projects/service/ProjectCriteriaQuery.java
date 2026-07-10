package org.openl.studio.projects.service;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Singular;

import org.openl.rules.project.abstraction.ProjectStatus;
import org.openl.studio.projects.model.ProjectIdModel;
import org.openl.studio.projects.model.ProjectInclude;

/**
 * Project criteria query. Used to filter projects in {@link ProjectService}.
 *
 * @author Vladyslav Pikus
 */
@Builder
public record ProjectCriteriaQuery(
        @Schema(description = "Identifier of the repository to filter projects by.")
        String repositoryId,

        @Schema(description = "Status of the projects to filter by.")
        ProjectStatus status,

        @Schema(description = "Identifiers of repositories to filter projects by.")
        List<String> repositoryIds,

        @Schema(description = "Statuses of the projects to filter by.")
        List<ProjectStatus> statuses,

        @Schema(description = "Identifier of the project that the returned projects depend on.")
        ProjectIdModel dependsOn,

        @Schema(description = "Set of tags to filter projects by.")
        @Singular
        Map<String, String> tags,

        @Schema(description = "Tag values to filter projects by.")
        Map<String, Set<String>> tagValues,

        @Schema(description = "Project name to filter by (partial match, case-insensitive).")
        String name,

        @Schema(description = "Author (last modifier) to filter by (partial match, case-insensitive).")
        String author,

        @Schema(description = "Branch to filter by (partial match, case-insensitive).")
        String branch,

        @Schema(description = "Field to sort the returned page by: name, status or updated.")
        String sort,

        @Schema(description = "Optional response expansions and listing behavior.")
        @Singular("include")
        List<ProjectInclude> includes
) {

    public static final String LOCAL_REPOSITORY_ID = "__local__";

    public ProjectCriteriaQuery {
        repositoryIds = normalizeRepositories(repositoryId, repositoryIds);
        statuses = normalizeStatuses(status, statuses);
        tags = tags != null ? Map.copyOf(tags) : Map.of();
        tagValues = normalizeTags(tags, tagValues);
        includes = ProjectInclude.normalize(includes);
    }

    public boolean hasRepositoryFilter() {
        return !repositoryIds.isEmpty();
    }

    public boolean localRepositorySelected() {
        return repositoryIds.contains(LOCAL_REPOSITORY_ID);
    }

    public List<String> designRepositoryIds() {
        return repositoryIds.stream()
                .filter(repository -> !LOCAL_REPOSITORY_ID.equals(repository))
                .toList();
    }

    public boolean hasStatusFilter() {
        return !statuses.isEmpty();
    }

    public boolean includeSummary() {
        return includes.contains(ProjectInclude.SUMMARY);
    }

    public boolean includeStatus() {
        return includes.contains(ProjectInclude.STATUS);
    }

    public boolean includeDeleted() {
        return includes.contains(ProjectInclude.DELETED);
    }

    public boolean includeModules() {
        return includes.contains(ProjectInclude.MODULES);
    }

    public ProjectCriteriaQuery withoutFacetFilters() {
        return ProjectCriteriaQuery.builder()
                .dependsOn(dependsOn)
                .name(name)
                .author(author)
                .branch(branch)
                .sort(sort)
                .includes(includes)
                .build();
    }

    private static List<String> normalizeRepositories(String repositoryId, List<String> repositoryIds) {
        var normalized = new LinkedHashSet<String>();
        addIfNotBlank(normalized, repositoryId);
        if (repositoryIds != null) {
            repositoryIds.forEach(repository -> addIfNotBlank(normalized, repository));
        }
        return List.copyOf(normalized);
    }

    private static List<ProjectStatus> normalizeStatuses(ProjectStatus status, List<ProjectStatus> statuses) {
        var normalized = new LinkedHashSet<ProjectStatus>();
        if (status != null) {
            normalized.add(status);
        }
        if (statuses != null) {
            statuses.stream()
                    .filter(Objects::nonNull)
                    .forEach(normalized::add);
        }
        return List.copyOf(normalized);
    }

    private static Map<String, Set<String>> normalizeTags(Map<String, String> tags,
                                                          Map<String, Set<String>> tagValues) {
        var normalized = new LinkedHashMap<String, Set<String>>();
        if (tags != null) {
            tags.forEach((tag, value) -> addTagValue(normalized, tag, value));
        }
        if (tagValues != null) {
            tagValues.forEach((tag, values) -> {
                if (values != null) {
                    values.forEach(value -> addTagValue(normalized, tag, value));
                }
            });
        }
        var immutable = new LinkedHashMap<String, Set<String>>();
        normalized.forEach((tag, values) -> immutable.put(tag, Set.copyOf(values)));
        return Map.copyOf(immutable);
    }

    private static void addIfNotBlank(Set<String> values, String value) {
        if (value != null && !value.isBlank()) {
            values.add(value);
        }
    }

    private static void addTagValue(Map<String, Set<String>> tags, String tag, String value) {
        if (tag != null && !tag.isBlank() && value != null && !value.isBlank()) {
            tags.computeIfAbsent(tag, ignored -> new LinkedHashSet<>()).add(value);
        }
    }
}
