package org.openl.studio.projects.model.trace;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

/**
 * The values of one watched cell across the whole run, one point per execution of its table.
 *
 * <p>A series is scoped to one cell in one table: if the same watched name exists in several tables, each
 * gets its own series. The points are ordered by execution, so reading them top to bottom is the value
 * across coverages or iterations. The UI can pivot several series that share a table into a matrix (rows
 * = executions, columns = cells); an agent can read a single series as a value sequence.
 *
 * @param name     the watched cell name (its {@code $...} step label)
 * @param table    display name of the table the cell belongs to
 * @param tableUri source URI of that table
 * @param points   the captured values, one per execution of the table, in execution order
 */
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "trace.type.watch-series.desc")
public record WatchSeriesView(
        @Schema(description = "trace.field.watch-series.name.desc")
        String name,

        @Schema(description = "trace.field.watch-series.table.desc")
        String table,

        @Schema(description = "trace.field.watch-series.table-uri.desc")
        String tableUri,

        @Schema(description = "trace.field.watch-series.points.desc")
        List<WatchPointView> points
) {
}
