package org.openl.studio.projects.model.trace;

import io.swagger.v3.oas.annotations.media.Schema;

import org.openl.rules.webstudio.web.trace.debug.FrameKind;

/**
 * One table in the profiling hotspots: its aggregated execution time across the whole run.
 *
 * <p>All invocations of the same table are folded into one row. {@code selfMillis} is the time spent in
 * the table's own code (excluding the tables it called); summed over every table it adds up to the
 * run's wall-clock time, which is what makes it the right column to sort by when looking for the slow
 * one. {@code totalMillis} is the inclusive time (own work plus called tables) and may exceed wall-clock
 * when a table runs inside another.
 *
 * @param uri        source URI of the table
 * @param name       display name of the table
 * @param kind       kind of the table
 * @param selfMillis total own execution time across all invocations, in milliseconds
 * @param totalMillis total inclusive execution time across all invocations, in milliseconds
 * @param count      number of times the table was invoked in the run
 */
@Schema(description = "trace.type.profile-hotspot.desc")
public record ProfileHotspotView(
        @Schema(description = "trace.field.profile-hotspot.uri.desc")
        String uri,

        @Schema(description = "trace.field.profile-hotspot.name.desc")
        String name,

        @Schema(description = "trace.field.profile-hotspot.kind.desc")
        FrameKind kind,

        @Schema(description = "trace.field.profile-hotspot.self.desc")
        double selfMillis,

        @Schema(description = "trace.field.profile-hotspot.total.desc")
        double totalMillis,

        @Schema(description = "trace.field.profile-hotspot.count.desc")
        int count
) {
}
