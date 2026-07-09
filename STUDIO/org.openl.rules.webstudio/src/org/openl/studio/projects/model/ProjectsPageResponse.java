package org.openl.studio.projects.model;

import java.util.Collection;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import org.openl.rules.repository.api.Pageable;
import org.openl.studio.common.model.PageResponse;
import org.openl.studio.projects.model.project.status.ProjectStatusViewModel;

/**
 * A page of projects plus optional list metadata.
 *
 * The summary counts ignore selected project facets and paging, so the client can render the filter rail,
 * workspace counters and total counters without loading every project. The counts are present only when
 * the client requests them via the summary flag.
 *
 * Compile statuses are present only when the client requests them and apply to the returned page content.
 */
public class ProjectsPageResponse extends PageResponse<ProjectViewModel> {

    private final ProjectStatusSummary statusCounts;
    private final List<FacetCount> repositoryCounts;
    private final List<TagFacetSummary> tagCounts;
    private final List<ProjectStatusViewModel> statuses;

    private ProjectsPageResponse(Collection<ProjectViewModel> content,
                                 Pageable page,
                                 Long total,
                                 ProjectStatusSummary statusCounts,
                                 List<FacetCount> repositoryCounts,
                                 List<TagFacetSummary> tagCounts,
                                 List<ProjectStatusViewModel> statuses) {
        super(content, page, total);
        this.statusCounts = statusCounts;
        this.repositoryCounts = repositoryCounts;
        this.tagCounts = tagCounts;
        this.statuses = statuses;
    }

    public static ProjectsPageResponse of(Collection<ProjectViewModel> content,
                                          Pageable page,
                                          long total,
                                          ProjectStatusSummary statusCounts,
                                          List<FacetCount> repositoryCounts,
                                          List<TagFacetSummary> tagCounts,
                                          List<ProjectStatusViewModel> statuses) {
        return new ProjectsPageResponse(content, page, total, statusCounts, repositoryCounts, tagCounts, statuses);
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public ProjectStatusSummary getStatusCounts() {
        return statusCounts;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public List<FacetCount> getRepositoryCounts() {
        return repositoryCounts;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public List<TagFacetSummary> getTagCounts() {
        return tagCounts;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public List<ProjectStatusViewModel> getStatuses() {
        return statuses;
    }
}
