package org.openl.studio.projects.model.tables;

import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * The resource a {@code merge} operation targets: a rectangular range of cells.
 *
 * @author Vladyslav Pikus
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = MergeTarget.Cells.class, name = "cells")
})
public sealed interface MergeTarget permits MergeTarget.Cells {

    @Schema(name = "MergeCells", description = "Merges a rectangular range of cells into one, keeping the value of the "
            + "top-left cell. The range must cover more than one cell and stay within the table.")
    record Cells(
            @Schema(description = "0-based row index of the top-left cell (0..height-1).")
            @NotNull
            Integer row,
            @Schema(description = "0-based column index of the top-left cell (0..width-1).")
            @NotNull
            Integer column,
            @Schema(description = "Number of rows the merged cell spans (>= 1).")
            @NotNull
            Integer rowspan,
            @Schema(description = "Number of columns the merged cell spans (>= 1).")
            @NotNull
            Integer colspan) implements MergeTarget {
    }

}
