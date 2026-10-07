package org.openl.studio.projects.model.tables;

import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * The resource an {@code append} operation adds: one or more rows or columns at the end of the table.
 *
 * @author Vladyslav Pikus
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = AppendTarget.Rows.class, name = "rows"),
        @JsonSubTypes.Type(value = AppendTarget.Columns.class, name = "columns")
})
public sealed interface AppendTarget permits AppendTarget.Rows, AppendTarget.Columns {

    @Schema(name = "AppendRows", description = """
            Adds one or more rows to the end of the table. Each new row must be \
            exactly as wide as the table. A rowspan may cover later rows from the same append block.""")
    record Rows(
            @NotEmpty
            @Parameter(description = """
                    New rows top to bottom, each a list of cells left to right. A cell may set \
                    colspan/rowspan to merge. Mark covered positions with `covered: true`. Each span must fit the \
                    table after the complete block is appended, and each row must be exactly as wide as the table.""")
            List<List<@Valid RawCellInput>> cells) implements AppendTarget {
    }

    @Schema(name = "AppendColumns", description = """
            Adds one or more columns to the end of the table. Each new column \
            must be exactly as tall as the table. A colspan may cover later columns from the same append block.""")
    record Columns(
            @NotEmpty
            @Parameter(description = """
                    New columns left to right, each a list of cells top to bottom. A cell may set \
                    colspan/rowspan to merge. Mark covered positions with `covered: true`. Each span must fit the \
                    table after the complete block is appended, and each column must be exactly as tall as the table.""")
            List<List<@Valid RawCellInput>> cells) implements AppendTarget {
    }

}
