package org.openl.studio.projects.model.trace;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import org.jspecify.annotations.Nullable;

import org.openl.rules.webstudio.web.trace.debug.DispatchInfo;
import org.openl.rules.webstudio.web.trace.debug.FrameKind;

/**
 * One frame of the live execution stack.
 *
 * @param index     position in the stack, 0 for the root call
 * @param depth     frame depth, 1 for the root call
 * @param uri       source URI of the table, used for breakpoints and table rendering
 * @param tableId   stable id of the table, used to fetch its raw grid from the Tables API
 * @param name      display name of the table
 * @param kind      kind of the table
 * @param location  current line inside the frame, or {@code null} at entry
 * @param active    whether this is the current (top) frame
 * @param completed whether the frame has returned
 * @param error     whether the frame failed
 * @param steps          the frame's sub-steps (spreadsheet cells or decision-table rules) with their status,
 *                       so the call tree shows every level at once; values are omitted (fetched per frame on demand)
 * @param durationMillis total execution time once the frame has returned, otherwise {@code null}
 * @param selfMillis     own execution time once the frame has returned (total minus called tables), otherwise {@code null}
 * @param dispatch       set when this table was selected by a dispatcher (overloaded by dimensions), otherwise {@code null}
 */
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "trace.type.debug-frame.desc")
public record DebugFrameView(
        @Schema(description = "trace.field.frame.index.desc")
        int index,

        @Schema(description = "trace.field.frame.depth.desc")
        int depth,

        @Schema(description = "trace.field.frame.uri.desc")
        String uri,

        @Schema(description = "trace.field.frame.table-id.desc")
        String tableId,

        @Schema(description = "trace.field.frame.name.desc")
        String name,

        @Schema(description = "trace.field.frame.kind.desc")
        FrameKind kind,

        @Schema(description = "trace.field.frame.location.desc")
        @Nullable DebugLocationView location,

        @Schema(description = "trace.field.frame.active.desc")
        boolean active,

        @Schema(description = "trace.field.frame.completed.desc")
        boolean completed,

        @Schema(description = "trace.field.frame.error.desc")
        boolean error,

        @Schema(description = "trace.field.frame.steps.desc")
        @Nullable List<StepValueView> steps,

        @Schema(description = "trace.field.frame.duration.desc")
        @Nullable Double durationMillis,

        @Schema(description = "trace.field.frame.self.desc")
        @Nullable Double selfMillis,

        @Schema(description = "trace.field.frame.dispatch.desc")
        @Nullable DispatchInfo dispatch
) {
}
