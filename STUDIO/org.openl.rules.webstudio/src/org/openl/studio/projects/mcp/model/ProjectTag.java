package org.openl.studio.projects.mcp.model;

import jakarta.validation.constraints.NotBlank;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Represents a project tag used for filtering projects in MCP tools.
 * Both name and value must be non-blank strings.
 */
public record ProjectTag(
        @Schema(description = "Tag name")
        @NotBlank
        String name,
        @Schema(description = "Tag value")
        @NotBlank
        String value
) {
}
