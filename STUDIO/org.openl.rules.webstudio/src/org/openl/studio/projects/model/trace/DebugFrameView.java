package org.openl.studio.projects.model.trace;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import org.jspecify.annotations.Nullable;

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
        boolean error
) {
}
