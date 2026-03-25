package org.openl.studio.projects.model.modules;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

/**
 * View model for a module's method filter configuration.
 */
@Builder
@Schema(description = "Method filter configuration for a module")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record MethodFilterView(
        @Schema(description = "Include patterns for method filtering")
        List<String> includes,

        @Schema(description = "Exclude patterns for method filtering")
        List<String> excludes
) {
}
