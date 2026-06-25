package org.openl.studio.projects.model.tables;

import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * The resource an {@code unmerge} operation targets: the merged cell covering a position.
 *
 * @author Vladyslav Pikus
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = UnmergeTarget.Cells.class, name = "cells")
})
public sealed interface UnmergeTarget permits UnmergeTarget.Cells {

    @Schema(name = "UnmergeCells", description = "Unmerges the merged cell that covers the given position, "
            + "splitting it back into individual cells.")
    record Cells(
            @Schema(description = "0-based row index of any cell in the merged region (0..height-1).")
            @NotNull
            Integer row,
            @Schema(description = "0-based column index of any cell in the merged region (0..width-1).")
            @NotNull
            Integer column) implements UnmergeTarget {
    }

}
