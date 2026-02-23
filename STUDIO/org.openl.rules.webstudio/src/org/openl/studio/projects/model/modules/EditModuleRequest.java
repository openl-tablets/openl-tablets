package org.openl.studio.projects.model.modules;

import java.util.List;
import jakarta.validation.constraints.NotBlank;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Request body for add and edit module operations.
 */
@Schema(description = "Request to add or edit a module in the project descriptor")
public record EditModuleRequest(

        @Schema(description = "Module name. Required for non-wildcard paths.")
        String name,

        @Schema(description = "File path relative to project root. For non-wildcard paths, the file must already exist.")
        @NotBlank
        String path,

        @Schema(description = "Include patterns for method filtering")
        List<String> includes,

        @Schema(description = "Exclude patterns for method filtering")
        List<String> excludes,

        @Schema(description = "If true, compile only this module")
        Boolean compileThisModuleOnly
) {
}
