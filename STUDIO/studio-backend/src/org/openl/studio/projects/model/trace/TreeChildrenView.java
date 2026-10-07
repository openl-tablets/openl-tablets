package org.openl.studio.projects.model.trace;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * One page of a step's executed sub-calls, fetched on demand so a large executed tree is never serialized
 * whole. The client loads the root one level deep, then fetches a step's children when it is expanded.
 *
 * @param children one level of the step's sub-calls from the requested offset — each shallow (its own steps,
 *                 but not its sub-calls)
 * @param total    the step's full sub-call count, so the client can page through the rest
 */
@Schema(description = "trace.type.tree-children.desc")
public record TreeChildrenView(
        @Schema(description = "trace.field.tree-children.children.desc")
        List<CallNodeView> children,

        @Schema(description = "trace.field.tree-children.total.desc")
        int total
) {
}
