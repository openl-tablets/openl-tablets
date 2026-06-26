package org.openl.studio.projects.model.tables;

import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * The resource an {@code append} operation adds: a row or a column.
 *
 * @author Vladyslav Pikus
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = AppendTarget.Row.class, name = "row"),
        @JsonSubTypes.Type(value = AppendTarget.Column.class, name = "column"),
        @JsonSubTypes.Type(value = AppendTarget.Rows.class, name = "rows"),
        @JsonSubTypes.Type(value = AppendTarget.Columns.class, name = "columns")
})
public sealed interface AppendTarget permits AppendTarget.Row, AppendTarget.Column, AppendTarget.Rows,
        AppendTarget.Columns {

    @Schema(name = "AppendRow", description = "Adds a row to the end of the table.")
    record Row(
            @NotEmpty
            @Valid
            @Parameter(description = "Row cells, left to right. A cell may set colspan/rowspan to merge. "
                    + "Must not be wider than the table.")
            List<RawCellInput> cells) implements AppendTarget {
    }

    @Schema(name = "AppendColumn", description = "Adds a column to the end of the table.")
    record Column(
            @NotEmpty
            @Valid
            @Parameter(description = "Column cells, top to bottom. A cell may set colspan/rowspan to merge. "
                    + "Must not be taller than the table.")
            List<RawCellInput> cells) implements AppendTarget {
    }

    @Schema(name = "AppendRows", description = "Adds a block of rows to the end of the table. The block must hold "
            + "more than one row, each as wide as the table.")
    record Rows(
            @NotEmpty
            @Parameter(description = "New rows top to bottom, each a list of cells left to right. More than one row, "
                    + "each exactly as wide as the table.")
            List<List<@Valid RawCellInput>> cells) implements AppendTarget {
    }

    @Schema(name = "AppendColumns", description = "Adds a block of columns to the end of the table. The block must "
            + "hold more than one column, each as tall as the table.")
    record Columns(
            @NotEmpty
            @Parameter(description = "New columns left to right, each a list of cells top to bottom. More than one "
                    + "column, each exactly as tall as the table.")
            List<List<@Valid RawCellInput>> cells) implements AppendTarget {
    }

}
