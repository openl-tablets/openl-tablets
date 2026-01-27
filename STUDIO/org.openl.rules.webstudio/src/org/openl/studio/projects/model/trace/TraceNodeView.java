package org.openl.studio.projects.model.trace;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Representation of a trace node for JSON response.
 */
@Schema(description = "Trace node view")
public record TraceNodeView(
        @Schema(description = "Unique key for this node, used for lazy loading children")
        int key,

        @Schema(description = "Display title of the node")
        String title,

        @Schema(description = "Tooltip text for the node")
        String tooltip,

        @Schema(description = "Type of the node (e.g., 'rule', 'condition', 'spreadsheet')")
        String type,

        @Schema(description = "If true, this node has children that can be loaded lazily")
        boolean lazy,

        @Schema(description = "CSS classes for styling (e.g., 'rule', 'condition result', 'condition fail')")
        String extraClasses
) {
}
