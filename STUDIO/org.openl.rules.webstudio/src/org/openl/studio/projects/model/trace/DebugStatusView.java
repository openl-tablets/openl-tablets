package org.openl.studio.projects.model.trace;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Lightweight debug session status, used for polling.
 *
 * @param status debug session status name
 */
@Schema(description = "trace.type.debug-status.desc")
public record DebugStatusView(
        @Schema(description = "trace.field.status.status.desc")
        String status
) {
}
