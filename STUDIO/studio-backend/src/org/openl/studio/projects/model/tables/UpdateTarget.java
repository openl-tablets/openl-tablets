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

    @Schema(name = "UpdateRow", description = """
            Overwrites the cells of an existing row, left to right. \
            The table is not resized.""")
    record Row(
            @Schema(description = "0-based index of the row to update (0..height-1).")
            @NotNull
            @Min(0)
            Integer position,
            @NotEmpty
            @Valid
            @Parameter(description = """
                    New row cells, left to right. A cell may set colspan/rowspan to merge. \
                    Must not be wider than the table.""")
            List<RawCellInput> cells) implements UpdateTarget {
    }

    @Schema(name = "UpdateColumn", description = """
            Overwrites the cells of an existing column, top to bottom. \
            The table is not resized.""")
    record Column(
            @Schema(description = "0-based index of the column to update (0..width-1).")
            @NotNull
            @Min(0)
            Integer position,
            @NotEmpty
            @Valid
            @Parameter(description = """
                    New column cells, top to bottom. A cell may set colspan/rowspan to merge. \
                    Must not be taller than the table.""")
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
            @Parameter(description = """
                    New cell value: a string, a number, a boolean or a one-dimensional array of these values. \
                    Null clears the cell. A text starting with '=' is an OpenL formula.""")
            @Schema(oneOf = {NullableCellValueSchema.class, Number.class, Boolean.class, CellValueArraySchema.class})
            @CellValueConstraint
            @Nullable Object value,
            @Valid
            @Parameter(description = """
                    Marks on pieces of a text value, each bold in its colour, in the order of the text and not \
                    overlapping. An empty list writes the text with no marks. Absent: a new value is written with no \
                    marks, and an unchanged value keeps the marks it has. A value written as a number, a date or a \
                    boolean keeps no marks. Refused for the header, for an Excel formula and in an .xls file.""")
            @Nullable List<@NotNull @Valid RawTextMark> marks,
            @Parameter(description = """
                    New Excel formula of the cell, as Excel writes it (for example '=B2*C2'). The cell is written as \
                    this Excel formula; a value sent beside it is refused.""")
            @Nullable String formula) implements UpdateTarget {

        /** Updates the value of a cell, leaving the marks of an unchanged value as they are. */
        public Cell(Integer row, Integer column, @Nullable Object value) {
            this(row, column, value, null, null);
        }

        /** Updates the value of a cell and the marks on its text. */
        public Cell(Integer row, Integer column, @Nullable Object value, @Nullable List<RawTextMark> marks) {
            this(row, column, value, marks, null);
        }
    }

    @Schema(name = "UpdateRange", description = """
            Overwrites a rectangular block of cells in place, anchored at the \
            top-left corner. The block must cover more than one cell and fit within the table; the table is not \
            resized.""")
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
            @Parameter(description = """
                    Block rows top to bottom, each a list of cells left to right. A cell may set \
                    colspan/rowspan to merge. Must be rectangular, cover more than one cell, and fit the table.""")
            List<List<@Valid RawCellInput>> cells) implements UpdateTarget {
    }

}
