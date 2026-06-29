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
 * The resource an {@code insert} operation adds: one or more rows or columns at a position.
 *
 * @author Vladyslav Pikus
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = InsertTarget.Rows.class, name = "rows"),
        @JsonSubTypes.Type(value = InsertTarget.Columns.class, name = "columns")
})
public sealed interface InsertTarget permits InsertTarget.Rows, InsertTarget.Columns {

    @Schema(name = "InsertRows", description = "Inserts one or more rows at the given position, shifting the rows at "
            + "and below it down. The first row is the header, so the position must be between 1 and the table height "
            + "(height appends to the end). Each new row must be exactly as wide as the table.")
    record Rows(
            @Schema(description = "0-based index the first new row will occupy (1..height; height appends to the end).")
            @NotNull
            @Min(1)
            Integer position,
            @NotEmpty
            @Parameter(description = "New rows top to bottom, each a list of cells left to right. A cell may set "
                    + "colspan/rowspan to merge. Each row must be exactly as wide as the table.")
            List<List<@Valid RawCellInput>> cells) implements InsertTarget {
    }

    @Schema(name = "InsertColumns", description = "Inserts one or more columns at the given position, shifting the "
            + "columns at and to the right of it. The first column carries the leading labels, so the position must be "
            + "between 1 and the table width (width appends to the end). Each new column must be exactly as tall as the "
            + "table.")
    record Columns(
            @Schema(description = "0-based index the first new column will occupy (1..width; width appends to the end).")
            @NotNull
            @Min(1)
            Integer position,
            @NotEmpty
            @Parameter(description = "New columns left to right, each a list of cells top to bottom. A cell may set "
                    + "colspan/rowspan to merge. Each column must be exactly as tall as the table.")
            List<List<@Valid RawCellInput>> cells) implements InsertTarget {
    }

}
