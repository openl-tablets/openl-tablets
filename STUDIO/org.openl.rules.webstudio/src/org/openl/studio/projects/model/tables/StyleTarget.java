package org.openl.studio.projects.model.tables;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * The resource a {@code style} operation targets: a rectangular range of cells.
 *
 * @author Vladyslav Pikus
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = StyleTarget.Cells.class, name = "cells")
})
public sealed interface StyleTarget permits StyleTarget.Cells {

    @Schema(name = "StyleCells", description = """
            Sets the styling of every cell of a rectangular range. The range must stay within the table; a \
            single cell is a range of one row by one column.""")
    record Cells(
            @Schema(description = "0-based row index of the top-left cell (0..height-1).")
            @NotNull
            @Min(0)
            Integer row,
            @Schema(description = "0-based column index of the top-left cell (0..width-1).")
            @NotNull
            @Min(0)
            Integer column,
            @Schema(description = "Number of rows the range spans (>= 1).")
            @NotNull
            @Min(1)
            Integer rowspan,
            @Schema(description = "Number of columns the range spans (>= 1).")
            @NotNull
            @Min(1)
            Integer colspan,
            @Schema(description = "Styling to set on every cell of the range.")
            @NotNull
            @Valid
            RawCellStyleInput style) implements StyleTarget {
    }

}
