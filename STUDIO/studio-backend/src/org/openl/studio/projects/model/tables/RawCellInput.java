package org.openl.studio.projects.model.tables;

import jakarta.validation.constraints.Min;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

/**
 * A cell to write into a table's raw source: its value or its Excel formula, plus optional merge spans.
 * <p>
 * This is the input counterpart of {@link RawTableCell}, without the read-only {@code cell} address that only makes
 * sense when reading. Spanning cells are described exactly as in the raw read: the origin cell carries
 * {@code colspan}/{@code rowspan}, and the cells it masks are marked {@code covered} so positions still map one-to-one
 * to columns (or rows).
 *
 * @param value   cell value as a scalar or one-dimensional scalar array; {@code null} is an empty cell
 * @param colspan number of columns this cell spans ({@code >= 2} to merge; null or 1 for a single column)
 * @param rowspan number of rows this cell spans ({@code >= 2} to merge; null or 1 for a single row)
 * @param covered marks a cell covered by another cell's span; its value is ignored
 * @param formula Excel formula of the cell, such as {@code =B2*C2}; refused together with a value
 * @author Vladyslav Pikus
 */
public record RawCellInput(
        @Parameter(description = """
                Cell value: a string, a number, a boolean or a one-dimensional array of these values. \
                A null value is an empty cell. A text starting with '=' is an OpenL formula.""")
        @Schema(oneOf = {NullableCellValueSchema.class, Number.class, Boolean.class, CellValueArraySchema.class})
        @CellValueConstraint
        @Nullable Object value,

        @Schema(description = "Number of columns this cell spans (>= 2 to merge; null or 1 for a single column).")
        @Min(1)
        @Nullable Integer colspan,

        @Schema(description = "Number of rows this cell spans (>= 2 to merge; null or 1 for a single row).")
        @Min(1)
        @Nullable Integer rowspan,

        @Schema(description = "Marks a cell covered by another cell's span; its value is ignored.")
        @Nullable Boolean covered,

        @Parameter(description = """
                Excel formula of the cell, as Excel writes it (for example '=B2*C2'). The cell is written as this \
                Excel formula; a value sent beside it is refused.""")
        @Nullable String formula
) {

    /** A cell holding a value, with optional merge spans. */
    public RawCellInput(@Nullable Object value, @Nullable Integer colspan, @Nullable Integer rowspan,
            @Nullable Boolean covered) {
        this(value, colspan, rowspan, covered, null);
    }
}
