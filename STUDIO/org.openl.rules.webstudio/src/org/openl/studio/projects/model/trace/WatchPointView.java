package org.openl.studio.projects.model.trace;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import org.jspecify.annotations.Nullable;

/**
 * One value of a watched cell, from one execution of its table.
 *
 * <p>The point ties the value to the execution it came from, so the value can be located and re-reached:
 * {@code instance} is the execution number, {@code path} the call path to it, and {@code ref} the
 * breakpoint key that stops on the cell (set it and replay to inspect that execution live).
 *
 * @param instance zero-based execution number of the owning table (its 1st, 2nd, ... invocation)
 * @param label    human-readable axis label for the execution, for example {@code "CoveragePremium #3"}
 * @param path     call path from the root frame to the owning frame, as table display names
 * @param ref      breakpoint key {@code uri#cellRef} that reaches this cell
 * @param value    the captured value: a scalar (number, string, boolean), or a short summary of a non-scalar
 */
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "trace.type.watch-point.desc")
public record WatchPointView(
        @Schema(description = "trace.field.watch-point.instance.desc")
        int instance,

        @Schema(description = "trace.field.watch-point.label.desc")
        String label,

        @Schema(description = "trace.field.watch-point.path.desc")
        List<String> path,

        @Schema(description = "trace.field.watch-point.ref.desc")
        String ref,

        @Schema(description = "trace.field.watch-point.value.desc")
        @Nullable Object value
) {
}
