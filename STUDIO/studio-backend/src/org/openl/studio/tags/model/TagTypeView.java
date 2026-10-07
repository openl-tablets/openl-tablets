package org.openl.studio.tags.model;

import java.util.List;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * A tag type offered when tagging a project: its name, its allowed values, and whether a value is
 * optional ({@code nullable}) or custom values beyond the listed ones are allowed ({@code extensible}).
 */
@Schema(description = "Tag type available for tagging projects")
public record TagTypeView(
        @Parameter(description = "Tag type name") String name,
        @Parameter(description = "Whether custom values beyond the listed ones are allowed") boolean extensible,
        @Parameter(description = "Whether leaving the tag value empty is allowed") boolean nullable,
        @Parameter(description = "Allowed values for this tag type") List<String> values) {
}
