package org.openl.studio.projects.model.tables;

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

    @Schema(name = "DeleteRow", description = "Deletes the row at the given position, shifting the rows below it up.")
    record Row(
            @Schema(description = "0-based index of the row to delete (0..height-1).")
            @NotNull
            Integer position) implements DeleteTarget {
    }

    @Schema(name = "DeleteColumn", description = "Deletes the column at the given position, shifting the columns to "
            + "its right left.")
    record Column(
            @Schema(description = "0-based index of the column to delete (0..width-1).")
            @NotNull
            Integer position) implements DeleteTarget {
    }

}
