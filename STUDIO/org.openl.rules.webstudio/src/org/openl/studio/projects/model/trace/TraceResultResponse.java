package org.openl.studio.projects.model.trace;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Response containing trace execution results.
 */
@Schema(description = "Trace result response")
public record TraceResultResponse(
        @Schema(description = "Root trace nodes (children of the trace root)")
        List<TraceNodeView> rootNodes,

        @Schema(description = "Total number of nodes in the trace tree")
        int totalNodes
) {
}
