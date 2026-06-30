package org.openl.studio.projects.model.trace;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import org.openl.rules.webstudio.web.trace.debug.FrameKind;

/**
 * A node of the executed call tree: a table invocation that has already returned.
 *
 * <p>Mirrors a live frame's shape — its sub-steps — but carries no values. It records only what ran and
 * the sub-calls each step made, so a returned branch can be browsed without retaining the data that
 * flowed through it. Present only when the session runs in profiling mode.
 *
 * @param uri            source URI of the table
 * @param name           display name of the table
 * @param kind           kind of the table
 * @param durationMillis total execution time in milliseconds (this table and everything it called), excluding parked time
 * @param selfMillis     own execution time in milliseconds (total minus the time spent in the tables it called)
 * @param steps          the executed sub-steps, each of which may carry its own executed sub-calls
 */
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "trace.type.call-node.desc")
public record CallNodeView(
        @Schema(description = "trace.field.call-node.uri.desc")
        String uri,

        @Schema(description = "trace.field.call-node.name.desc")
        String name,

        @Schema(description = "trace.field.call-node.kind.desc")
        FrameKind kind,

        @Schema(description = "trace.field.call-node.duration.desc")
        double durationMillis,

        @Schema(description = "trace.field.call-node.self.desc")
        double selfMillis,

        @Schema(description = "trace.field.call-node.steps.desc")
        List<StepValueView> steps
) {
}
