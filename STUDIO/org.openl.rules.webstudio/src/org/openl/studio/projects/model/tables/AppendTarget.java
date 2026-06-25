package org.openl.studio.projects.model.tables;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * The resource an {@code append} operation adds: a row or a column.
 *
 * @author Vladyslav Pikus
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = AppendTarget.Row.class, name = "row"),
        @JsonSubTypes.Type(value = AppendTarget.Column.class, name = "column")
})
public sealed interface AppendTarget permits AppendTarget.Row, AppendTarget.Column {

    @Schema(name = "AppendRow", description = "Adds a row to the end of the table.")
    record Row(
            @Schema(description = "Row cells, left to right. A cell may set colspan/rowspan to merge. "
                    + "Must not be wider than the table.")
            List<RawCellInput> cells) implements AppendTarget {
    }

    @Schema(name = "AppendColumn", description = "Adds a column to the end of the table.")
    record Column(
            @Schema(description = "Column cells, top to bottom. A cell may set colspan/rowspan to merge. "
                    + "Must not be taller than the table.")
            List<RawCellInput> cells) implements AppendTarget {
    }

}
