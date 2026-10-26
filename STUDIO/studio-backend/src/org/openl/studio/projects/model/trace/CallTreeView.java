package org.openl.studio.projects.model.trace;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

/**
 * A part of the executed tree in one flat list: the calls of one step and the levels below them.
 *
 * <p>A call made by the requested step names no caller. Every other call names its caller by position and step,
 * and comes after it. The list is bounded: a step whose calls are left out reports how many it made, so its branch
 * is loaded in turn.
 *
 * @param nodes      the calls, the levels nearest the step first
 * @param dispatches the distinct dispatches of the calls, which a node names by position; {@code null} when no call
 *                   was dispatched
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "trace.type.call-tree.desc")
public record CallTreeView(
        @Parameter(description = "trace.field.call-tree.nodes.desc")
        List<CallNodeView> nodes,

        @Parameter(description = "trace.field.call-tree.dispatches.desc")
        @Nullable List<DispatchInfo> dispatches
) {
}
