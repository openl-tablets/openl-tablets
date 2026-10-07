package org.openl.studio.projects.model;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * A calculated spreadsheet laid out the way its author wrote it.
 *
 * <p>A spreadsheet result travels as the bean OpenL publishes for it, whose properties are named after the cells.
 * This view says which cell each value belongs to, so that a client can show the result as the table it comes
 * from instead of a flat list of properties.
 *
 * <p>The values are given row by row, in the order the rows and the columns are named. Every row holds one value
 * per column, and a cell that holds nothing carries no value.
 *
 * @param columns names of the spreadsheet columns
 * @param rows    names of the spreadsheet rows, also called steps
 * @param cells   calculated values, row by row
 */
@Schema(description = "A calculated spreadsheet laid out by its rows and columns")
public record SpreadsheetResultView(
        @Schema(description = "Names of the spreadsheet columns")
        List<String> columns,

        @Schema(description = "Names of the spreadsheet rows, also called steps")
        List<String> rows,

        @Schema(description = "Calculated cell values, row by row")
        List<List<JsonNode>> cells
) {
}
