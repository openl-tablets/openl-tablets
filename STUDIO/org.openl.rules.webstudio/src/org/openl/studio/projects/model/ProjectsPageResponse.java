package org.openl.studio.projects.model;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.Getter;

import org.openl.rules.repository.api.Pageable;
import org.openl.rules.workspace.dtr.BranchedProjectIndexService.IndexHealth;
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
@Getter
public class ProjectsPageResponse extends PageResponse<ProjectViewModel> {

    @Parameter(description = "Per-status project counts. Present only when the summary is requested.")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final ProjectStatusSummary statusCounts;

    @Parameter(description = "Per-repository project counts. Present only when the summary is requested.")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final List<FacetCount> repositoryCounts;

    @Parameter(description = "Per-tag-type value counts. Present only when the summary is requested.")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final List<TagFacetSummary> tagCounts;

    @Parameter(description = "Compilation statuses for the returned page. Present only when requested.")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final List<ProjectStatusViewModel> statuses;

    @Parameter(description = "Cross-branch project-index health by readable design repository.")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private final Map<String, IndexHealth> projectIndexHealth;

    private ProjectsPageResponse(Collection<ProjectViewModel> content,
                                 Pageable page,
                                 Long total,
                                 ProjectStatusSummary statusCounts,
                                 List<FacetCount> repositoryCounts,
                                 List<TagFacetSummary> tagCounts,
                                 List<ProjectStatusViewModel> statuses,
                                 Map<String, IndexHealth> projectIndexHealth) {
        super(content, page, total);
        this.statusCounts = statusCounts;
        this.repositoryCounts = repositoryCounts;
        this.tagCounts = tagCounts;
        this.statuses = statuses;
        this.projectIndexHealth = Map.copyOf(projectIndexHealth);
    }

    public static ProjectsPageResponse of(Collection<ProjectViewModel> content,
                                          Pageable page,
                                          long total,
                                          ProjectStatusSummary statusCounts,
                                          List<FacetCount> repositoryCounts,
                                          List<TagFacetSummary> tagCounts,
                                          List<ProjectStatusViewModel> statuses,
                                          Map<String, IndexHealth> projectIndexHealth) {
        return new ProjectsPageResponse(
                content,
                page,
                total,
                statusCounts,
                repositoryCounts,
                tagCounts,
                statuses,
                projectIndexHealth);
    }
}
