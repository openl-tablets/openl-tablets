package org.openl.studio.projects.model.merge;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;

import org.openl.studio.projects.model.tables.RawTableCell;
import org.openl.studio.projects.model.tables.RawTableCellStyle;

/**
 * A table as it stands in one version of a conflicted workbook.
 *
 * @param range        the place of the table on its sheet, in A1 notation
 * @param cells        the cells of the table row by row; the first rows only, when the table has more of them than a
 *                     screen shows
 * @param styles       the Excel styles the cells name by index, each once
 * @param rows         how many rows the table has
 * @param changedCells  the cells whose value, formula, comment or merging the branch changed against the base
 *                      version, and the other branch has not alike, in A1 notation: the cells that hold something
 *                      else, or every cell of a row or a column the branch inserted or changed when it inserted or
 *                      deleted rows or columns; none for the base version, or for a table whose numbers of both rows
 *                      and columns differ from the base one
 * @param formattingChanged whether the branch changed the formatting of cells of the table, and the other branch has
 *                      not alike; such cells are not in {@code changedCells}
 * @param conflictCells the cells whose value, formula, comment or merging both branches changed differently, in A1
 *                      notation as this version has them; none when the versions differ in size
 */
@Schema(description = "A table as it stands in one version of a conflicted workbook")
public record ConflictTableVersion(
        @Parameter(description = "The place of the table on its sheet, in A1 notation")
        String range,

        @Parameter(description = "The cells of the table, row by row. A table of more than 200 rows has its first 200 "
                + "rows here")
        @JsonInclude(JsonInclude.Include.ALWAYS)
        List<List<RawTableCell>> cells,

        @Parameter(description = "The Excel styles the cells of the table use, each once: a cell and a piece of its "
                + "text name their style by its index in this list")
        @JsonInclude(JsonInclude.Include.ALWAYS)
        List<RawTableCellStyle> styles,

        @Parameter(description = "How many rows the table has")
        int rows,

        @Parameter(description = "The cells whose value, formula, comment or merging the branch changed against "
                + "the base version, and the other branch has not alike, in A1 notation: the cells that hold something "
                + "else, or, in a table with rows or columns inserted or deleted, every cell of a row or a column the "
                + "branch inserted or changed. Empty for the base version, and for a table whose numbers of both rows "
                + "and columns differ from the base one")
        @JsonInclude(JsonInclude.Include.ALWAYS)
        List<String> changedCells,

        @Parameter(description = "Whether the branch changed the formatting of cells of the table, and the other "
                + "branch has not alike. Such cells are not listed in changedCells")
        boolean formattingChanged,

        @Parameter(description = "The cells whose value, formula, comment or merging both branches changed "
                + "differently, in A1 notation as this version has them. Empty when the versions of the table differ "
                + "in size")
        @JsonInclude(JsonInclude.Include.ALWAYS)
        List<String> conflictCells
) {
}
