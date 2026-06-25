package org.openl.studio.projects.model.tables;

import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

/**
 * A cell to write into a table's raw source: its value plus optional merge spans.
 * <p>
 * This is the input counterpart of {@link RawTableCell}, without the read-only {@code cell} address that only makes
 * sense when reading. Spanning cells are described exactly as in the raw read: the origin cell carries
 * {@code colspan}/{@code rowspan}, and the cells it masks are marked {@code covered} so positions still map one-to-one
 * to columns (or rows).
 *
 * @param value   cell value; {@code null} is an empty cell
 * @param colspan number of columns this cell spans ({@code >= 2} to merge; null or 1 for a single column)
 * @param rowspan number of rows this cell spans ({@code >= 2} to merge; null or 1 for a single row)
 * @param covered marks a cell covered by another cell's span; its value is ignored
 * @author Vladyslav Pikus
 */
public record RawCellInput(
        @Schema(description = "Cell value. A null value is an empty cell.")
        @Nullable Object value,

        @Schema(description = "Number of columns this cell spans (>= 2 to merge; null or 1 for a single column).")
        @Nullable Integer colspan,

        @Schema(description = "Number of rows this cell spans (>= 2 to merge; null or 1 for a single row).")
        @Nullable Integer rowspan,

        @Schema(description = "Marks a cell covered by another cell's span; its value is ignored.")
        @Nullable Boolean covered
) {
}
