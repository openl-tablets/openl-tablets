package org.openl.studio.projects.model;

import java.util.Map;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;

import org.openl.rules.project.abstraction.ProjectStatus;
import org.openl.studio.common.projection.NoFieldProjection;

/**
 * Per-status project counts for the projects summary.
 *
 * <p>Field names are the camelCase status codes. {@code VIEWING} is reported as {@code opened} to match the
 * status vocabulary of the {@code status} field. Every count is always present; a status with no projects
 * is reported as {@code 0}.
 */
@NoFieldProjection
@Schema(description = "Number of projects per status across the query scope, ignoring the status filter and paging.")
public record ProjectStatusSummary(
        @Parameter(description = "Number of local-only projects") long local,
        @Parameter(description = "Number of opened projects with no local changes") long opened,
        @Parameter(description = "Number of projects opened at a historical revision") long viewingVersion,
        @Parameter(description = "Number of projects opened for editing") long editing,
        @Parameter(description = "Number of closed projects") long closed,
        @Parameter(description = "Number of deleted (archived) projects") long deleted) {

    public static ProjectStatusSummary of(Map<ProjectStatus, Long> counts) {
        return new ProjectStatusSummary(
                counts.getOrDefault(ProjectStatus.LOCAL, 0L),
                counts.getOrDefault(ProjectStatus.VIEWING, 0L),
                counts.getOrDefault(ProjectStatus.VIEWING_VERSION, 0L),
                counts.getOrDefault(ProjectStatus.EDITING, 0L),
                counts.getOrDefault(ProjectStatus.CLOSED, 0L),
                counts.getOrDefault(ProjectStatus.DELETED, 0L));
    }
}
