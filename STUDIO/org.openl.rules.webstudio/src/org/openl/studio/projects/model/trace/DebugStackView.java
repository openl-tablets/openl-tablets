package org.openl.studio.projects.model.trace;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import org.jspecify.annotations.Nullable;

import org.openl.rules.webstudio.web.trace.debug.DebugStatus;

/**
 * The live execution stack at the current suspension.
 *
 * @param status  debug session status
 * @param frames  stack frames ordered from the root call to the current frame
 * @param error   failure detail when the session ended in error, otherwise {@code null}
 * @param tree    the whole executed tree once the trace has finished (profiling mode), so it outlives the
 *                now-empty stack; {@code null} while the trace is still running, or when the caller asked
 *                to omit it
 * @param profile a bounded overview of the finished profiled run (the slowest tables), so a large run can
 *                be understood without pulling the full {@code tree}; {@code null} outside profiling mode
 *                and while the trace is still running
 */
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "trace.type.debug-stack.desc")
public record DebugStackView(
        @Schema(description = "trace.field.stack.status.desc")
        DebugStatus status,

        @Schema(description = "trace.field.stack.frames.desc")
        List<DebugFrameView> frames,

        @Schema(description = "trace.field.stack.error.desc")
        @Nullable DebugError error,

        @Schema(description = "trace.field.stack.tree.desc")
        @Nullable CallNodeView tree,

        @Schema(description = "trace.field.stack.profile.desc")
        @Nullable ProfileSummaryView profile
) {
}
