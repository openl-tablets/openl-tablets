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

/**
 * The resource an {@code insert} operation adds: a row or a column at a position.
 *
 * @author Vladyslav Pikus
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = InsertTarget.Row.class, name = "row"),
        @JsonSubTypes.Type(value = InsertTarget.Column.class, name = "column"),
        @JsonSubTypes.Type(value = InsertTarget.Rows.class, name = "rows"),
        @JsonSubTypes.Type(value = InsertTarget.Columns.class, name = "columns")
})
public sealed interface InsertTarget permits InsertTarget.Row, InsertTarget.Column, InsertTarget.Rows,
        InsertTarget.Columns {

    @Schema(name = "InsertRow", description = "Inserts a row at the given position, shifting the rows at and below it "
            + "down. The first row is the header, so the position must be between 1 and the table height "
            + "(height appends to the end).")
    record Row(
            @Schema(description = "0-based index the new row will occupy (1..height; height appends to the end).")
            @NotNull
            @Min(1)
            Integer position,
            @NotEmpty
            @Valid
            @Parameter(description = "Row cells, left to right. A cell may set colspan/rowspan to merge. "
                    + "Must not be wider than the table.")
            List<RawCellInput> cells) implements InsertTarget {
    }

    @Schema(name = "InsertColumn", description = "Inserts a column at the given position, shifting the columns at and "
            + "to the right of it. The first column carries the leading labels, so the position must be between 1 and "
            + "the table width (width appends to the end).")
    record Column(
            @Schema(description = "0-based index the new column will occupy (1..width; width appends to the end).")
            @NotNull
            @Min(1)
            Integer position,
            @NotEmpty
            @Valid
            @Parameter(description = "Column cells, top to bottom. A cell may set colspan/rowspan to merge. "
                    + "Must not be taller than the table.")
            List<RawCellInput> cells) implements InsertTarget {
    }

    @Schema(name = "InsertRows", description = "Inserts a block of rows at the given position, shifting the rows at "
            + "and below it down. The block must hold more than one row, each as wide as the table.")
    record Rows(
            @Schema(description = "0-based index the first new row will occupy (1..height; height appends to the end).")
            @NotNull
            @Min(1)
            Integer position,
            @NotEmpty
            @Parameter(description = "New rows top to bottom, each a list of cells left to right. More than one row, "
                    + "each exactly as wide as the table.")
            List<List<@Valid RawCellInput>> cells) implements InsertTarget {
    }

    @Schema(name = "InsertColumns", description = "Inserts a block of columns at the given position, shifting the "
            + "columns at and to the right of it. The block must hold more than one column, each as tall as the table.")
    record Columns(
            @Schema(description = "0-based index the first new column will occupy (1..width; width appends to the end).")
            @NotNull
            @Min(1)
            Integer position,
            @NotEmpty
            @Parameter(description = "New columns left to right, each a list of cells top to bottom. More than one "
                    + "column, each exactly as tall as the table.")
            List<List<@Valid RawCellInput>> cells) implements InsertTarget {
    }

}
