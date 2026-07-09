package org.openl.studio.projects.model;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

import org.openl.studio.common.projection.NoFieldProjection;

/**
 * Counts for all values of one project tag type.
 */
@NoFieldProjection
@Schema(description = "Counts for all values of one project tag type.")
public record TagFacetSummary(String type, List<FacetCount> values) {

    public TagFacetSummary {
        values = values == null ? List.of() : List.copyOf(values);
    }
}
