package org.openl.studio.projects.model.trace;

import io.swagger.v3.oas.annotations.media.Schema;

import org.openl.rules.webstudio.web.trace.debug.DebugStatus;

/**
 * Lightweight debug session status, used for polling.
 *
 * @param status debug session status
 */
@Schema(description = "trace.type.debug-status.desc")
public record DebugStatusView(
        @Schema(description = "trace.field.status.status.desc")
        DebugStatus status
) {
}
