package org.openl.studio.projects.model.trace;

import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Request body for trace execution with JSON input parameters.
 */
@Schema(description = "Trace input request")
public record TraceInputRequest(
        @Schema(description = "Runtime context for rule execution")
        Map<String, Object> runtimeContext,

        @Schema(description = "Input parameters as JSON object, where keys are parameter names")
        Map<String, Object> params
) {
}
