package org.openl.studio.projects.model.tables;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * The resource a {@code delete} operation removes: one or more rows or columns at a position.
 *
 * @author Vladyslav Pikus
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = DeleteTarget.Rows.class, name = "rows"),
        @JsonSubTypes.Type(value = DeleteTarget.Columns.class, name = "columns")
})
public sealed interface DeleteTarget permits DeleteTarget.Rows, DeleteTarget.Columns {

    @Schema(name = "DeleteRows", description = "Deletes one or more rows starting at the given position, shifting the "
            + "rows below the block up. The header row (position 0) cannot be deleted, and the block must stay within "
            + "the body.")
    record Rows(
            @Schema(description = "0-based index of the first body row to delete (1..height-1).")
            @NotNull
            @Min(1)
            Integer position,
            @Schema(description = "Number of rows to delete (>= 1).")
            @NotNull
            @Min(1)
            Integer count) implements DeleteTarget {
    }

    @Schema(name = "DeleteColumns", description = "Deletes one or more columns starting at the given position, "
            + "shifting the columns to the right of the block left. The leading-label column (position 0) cannot be "
            + "deleted, and the block must stay within the body.")
    record Columns(
            @Schema(description = "0-based index of the first column to delete (1..width-1).")
            @NotNull
            @Min(1)
            Integer position,
            @Schema(description = "Number of columns to delete (>= 1).")
            @NotNull
            @Min(1)
            Integer count) implements DeleteTarget {
    }

}
