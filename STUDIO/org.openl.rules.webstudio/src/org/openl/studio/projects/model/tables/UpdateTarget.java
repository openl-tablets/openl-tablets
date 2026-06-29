package org.openl.studio.projects.model.tables;

import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

/**
 * The resource an {@code update} operation overwrites: a row, a column or a single cell.
 *
 * @author Vladyslav Pikus
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = UpdateTarget.Row.class, name = "row"),
        @JsonSubTypes.Type(value = UpdateTarget.Column.class, name = "column"),
        @JsonSubTypes.Type(value = UpdateTarget.Cell.class, name = "cell"),
        @JsonSubTypes.Type(value = UpdateTarget.Range.class, name = "range")
})
public sealed interface UpdateTarget permits UpdateTarget.Row, UpdateTarget.Column, UpdateTarget.Cell,
        UpdateTarget.Range {

    @Schema(name = "UpdateRow", description = "Overwrites the cells of an existing row, left to right. "
            + "The table is not resized.")
    record Row(
            @Schema(description = "0-based index of the row to update (0..height-1).")
            @NotNull
            @Min(0)
            Integer position,
            @NotEmpty
            @Valid
            @Parameter(description = "New row cells, left to right. A cell may set colspan/rowspan to merge. "
                    + "Must not be wider than the table.")
            List<RawCellInput> cells) implements UpdateTarget {
    }

    @Schema(name = "UpdateColumn", description = "Overwrites the cells of an existing column, top to bottom. "
            + "The table is not resized.")
    record Column(
            @Schema(description = "0-based index of the column to update (0..width-1).")
            @NotNull
            @Min(0)
            Integer position,
            @NotEmpty
            @Valid
            @Parameter(description = "New column cells, top to bottom. A cell may set colspan/rowspan to merge. "
                    + "Must not be taller than the table.")
            List<RawCellInput> cells) implements UpdateTarget {
    }

    @Schema(name = "UpdateCell", description = "Updates the value of a single existing cell.")
    record Cell(
            @Schema(description = "0-based row index (0..height-1).")
            @NotNull
            @Min(0)
            Integer row,
            @Schema(description = "0-based column index (0..width-1).")
            @NotNull
            @Min(0)
            Integer column,
            @Schema(description = "New cell value: a string, a number or a boolean. Null clears the cell.",
                    oneOf = {String.class, Number.class, Boolean.class})
            @CellValueConstraint
            @Nullable Object value) implements UpdateTarget {
    }

    @Schema(name = "UpdateRange", description = "Overwrites a rectangular block of cells in place, anchored at the "
            + "top-left corner. The block must cover more than one cell and fit within the table; the table is not "
            + "resized.")
    record Range(
            @Schema(description = "0-based row index of the top-left cell (0..height-1).")
            @NotNull
            @Min(0)
            Integer row,
            @Schema(description = "0-based column index of the top-left cell (0..width-1).")
            @NotNull
            @Min(0)
            Integer column,
            @NotEmpty
            @Parameter(description = "Block rows top to bottom, each a list of cells left to right. A cell may set "
                    + "colspan/rowspan to merge. Must be rectangular, cover more than one cell, and fit the table.")
            List<List<@Valid RawCellInput>> cells) implements UpdateTarget {
    }

}
