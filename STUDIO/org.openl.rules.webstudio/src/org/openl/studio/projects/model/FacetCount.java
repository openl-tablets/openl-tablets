package org.openl.studio.projects.model;

import io.swagger.v3.oas.annotations.media.Schema;

import org.openl.studio.common.projection.NoFieldProjection;

/**
 * Count for one selectable facet value in a projects summary.
 */
@NoFieldProjection
@Schema(description = "Count for one selectable facet value in a projects summary.")
public record FacetCount(String id, String name, long count) {
}
