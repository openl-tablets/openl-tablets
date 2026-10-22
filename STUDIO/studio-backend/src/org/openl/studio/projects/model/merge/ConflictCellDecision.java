package org.openl.studio.projects.model.merge;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

/**
 * A decision for one cell of a merged table, named by its row and its column counted from 0 as the {@code cells} of
 * the merged table are: the version a conflicting cell takes, a value typed for any cell, or both.
 *
 * @param row    the row of the cell
 * @param column the column of the cell
 * @param side   the version the cell takes its value and its comment from, when both branches changed them
 *               differently; {@code null} when none is decided for the cell itself
 * @param value  the value typed for the cell, read as Excel reads what is typed into a cell, or {@code null} when none
 */
@Schema(description = """
        A decision for one cell of a merged table: the version a conflicting cell takes, a value typed \
        for any cell, or both""")
public record ConflictCellDecision(
        @Parameter(description = "The row of the cell in the merged table, counted from 0", required = true)
        @NotNull
        @Min(0)
        Integer row,

        @Parameter(description = "The column of the cell in the merged table, counted from 0", required = true)
        @NotNull
        @Min(0)
        Integer column,

        @Parameter(description = """
                The version a conflicting cell takes its value and its comment from: `ours` or \
                `theirs`""")
        @Nullable ConflictBase side,

        @Parameter(description = """
                The value typed for the cell, which takes the place of its value and keeps its \
                formatting. It is read as Excel reads what is typed into a cell: a text after `=` is a formula, \
                `TRUE` and `FALSE` are booleans, a number is written with a dot, a date as `2026-10-22` or \
                `2026-10-22 14:30`, a text after an apostrophe stays text, and an empty value clears the cell""")
        @Nullable String value
) {
}
