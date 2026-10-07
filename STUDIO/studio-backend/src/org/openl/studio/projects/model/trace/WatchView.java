package org.openl.studio.projects.model.trace;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

/**
 * The collected values of the watched cells: a factor read across the whole run.
 *
 * <p>A watch retains the value of named cells on every execution of their table, so a factor can be read
 * across all coverages or iterations without pulling every frame. The series are complete once the run
 * has finished; while it is still running they carry the executions seen so far.
 *
 * @param series    one series per watched cell (scoped to its table), each with a value per execution
 * @param truncated whether the capture cap was reached, so some late executions are missing
 */
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "trace.type.watch.desc")
public record WatchView(
        @Schema(description = "trace.field.watch.series.desc")
        List<WatchSeriesView> series,

        @Schema(description = "trace.field.watch.truncated.desc")
        boolean truncated
) {
}
