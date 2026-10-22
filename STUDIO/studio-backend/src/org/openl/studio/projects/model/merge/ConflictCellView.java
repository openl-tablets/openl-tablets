package org.openl.studio.projects.model.merge;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;

import org.openl.studio.projects.model.tables.RawTableCell;

/**
 * A cell of a merged table both branches changed differently, with what it is once a version is decided for it.
 *
 * @param row    the row of the cell in the merged table, counted from 0
 * @param column the column of the cell in the merged table, counted from 0
 * @param cell      the cell on its sheet, in A1 notation
 * @param ourCell   the cell in your version, in A1 notation; it differs from the cell when a branch inserted or
 *                  removed rows or columns before it
 * @param theirCell the cell in their version, in A1 notation; it differs from the cell when a branch inserted or
 *                  removed rows or columns before it
 * @param ours      the cell as it is with your version: your value and comment, the formatting of the merged cell
 * @param theirs    the cell as it is with their version: their value and comment, the formatting of the merged cell
 */
@Schema(description = """
        A cell of a merged table both branches changed differently, with what it is once a version is \
        decided for it""")
public record ConflictCellView(
        @Parameter(description = "The row of the cell in the merged table, counted from 0")
        int row,

        @Parameter(description = "The column of the cell in the merged table, counted from 0")
        int column,

        @Parameter(description = "The cell on its sheet, in A1 notation")
        String cell,

        @Parameter(description = """
                The cell in your version, in A1 notation; it differs from the cell when a branch inserted or \
                removed rows or columns before it""")
        String ourCell,

        @Parameter(description = """
                The cell in their version, in A1 notation; it differs from the cell when a branch inserted or \
                removed rows or columns before it""")
        String theirCell,

        @Parameter(description = """
                The cell as it is with your version: your value and comment, in the formatting of \
                the merged cell, named by index in the styles of the merged table""")
        RawTableCell ours,

        @Parameter(description = """
                The cell as it is with their version: their value and comment, in the formatting of \
                the merged cell, named by index in the styles of the merged table""")
        RawTableCell theirs
) {
}
