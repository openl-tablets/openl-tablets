package org.openl.studio.projects.model.tables;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * The resource a {@code delete} operation removes: a row or a column at a position.
 *
 * @author Vladyslav Pikus
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = DeleteTarget.Row.class, name = "row"),
        @JsonSubTypes.Type(value = DeleteTarget.Column.class, name = "column")
})
public sealed interface DeleteTarget permits DeleteTarget.Row, DeleteTarget.Column {

    @Schema(name = "DeleteRow", description = "Deletes the row at the given position, shifting the rows below it up. "
            + "The header row (position 0) cannot be deleted.")
    record Row(
            @Schema(description = "0-based index of the body row to delete (1..height-1).")
            @NotNull
            @Min(1)
            Integer position) implements DeleteTarget {
    }

    @Schema(name = "DeleteColumn", description = "Deletes the column at the given position, shifting the columns to "
            + "its right left. The leading-label column (position 0) cannot be deleted.")
    record Column(
            @Schema(description = "0-based index of the column to delete (1..width-1).")
            @NotNull
            @Min(1)
            Integer position) implements DeleteTarget {
    }

}
