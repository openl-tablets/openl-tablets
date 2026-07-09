package org.openl.studio.tags.model;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * A tag type offered when tagging a project: its name, its allowed values, and whether a value is
 * optional ({@code nullable}) or custom values beyond the listed ones are allowed ({@code extensible}).
 */
@Schema(description = "Tag type available for tagging projects")
public record TagTypeView(String name, boolean extensible, boolean nullable, List<String> values) {
}
