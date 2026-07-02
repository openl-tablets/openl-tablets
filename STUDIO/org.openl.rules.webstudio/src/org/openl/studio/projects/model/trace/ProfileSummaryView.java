package org.openl.studio.projects.model.trace;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

/**
 * A bounded overview of a profiled run: the slowest tables, so a whole run can be understood without
 * pulling the full executed tree.
 *
 * <p>It aggregates the executed call tree by table — folding every invocation of the same table into one
 * hotspot — and keeps only the slowest {@code hotspots} by own time. This makes the response small and
 * constant-sized regardless of how large the run was, which the full {@link CallNodeView} tree is not.
 * Fetch the full tree only to drill into a specific branch.
 *
 * @param hotspots       the slowest tables by own time, most expensive first, capped to the requested size
 * @param distinctTables number of distinct tables that ran (may exceed the number of hotspots returned)
 * @param nodeCount      total number of table invocations in the run (the size of the full tree)
 * @param totalMillis    wall-clock execution time of the whole run, in milliseconds, excluding parked time
 * @param truncated      whether the profile is incomplete: more distinct tables ran than the returned hotspots, or
 *                       the executed tree hit its node cap
 */
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "trace.type.profile-summary.desc")
public record ProfileSummaryView(
        @Schema(description = "trace.field.profile.hotspots.desc")
        List<ProfileHotspotView> hotspots,

        @Schema(description = "trace.field.profile.distinct-tables.desc")
        int distinctTables,

        @Schema(description = "trace.field.profile.node-count.desc")
        int nodeCount,

        @Schema(description = "trace.field.profile.total.desc")
        double totalMillis,

        @Schema(description = "trace.field.profile.truncated.desc")
        boolean truncated
) {
}
