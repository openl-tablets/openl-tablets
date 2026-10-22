package org.openl.studio.projects.model.merge;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;

import org.openl.studio.projects.model.tables.RawTableCell;
import org.openl.studio.projects.model.tables.RawTableCellStyle;

/**
 * A conflicting table merged cell by cell: the cells only one branch changed, or both changed alike, are merged as
 * Sync merges them, and the cells both branches changed differently are left to decide.
 *
 * <p>The rows and the columns of the merged table, counted from 0, name its cells in a decision.
 *
 * @param range        the place of the merged table on its sheet, in A1 notation
 * @param cells        the cells of the merged table row by row, the first rows only of a long table; a conflicting
 *                     cell holds the value and the comment of the base version until a version is decided for it
 * @param styles       the Excel styles the cells name by index, each once
 * @param rows         how many rows the merged table has
 * @param changedCells the cells of the rows shown whose value, formula or comment one branch alone changed, which the
 *                     merge takes from that branch by itself, in A1 notation; the cells of a row or a column one
 *                     branch alone has included
 * @param conflicts    the cells both branches changed differently, row by row, each as it is with either version
 */
@Schema(description = """
        A conflicting table merged cell by cell: the cells only one branch changed, or both changed \
        alike, merged, and the cells both branches changed differently left to decide""")
public record ConflictTableMerge(
        @Parameter(description = "The place of the merged table on its sheet, in A1 notation")
        String range,

        @Parameter(description = """
                The cells of the merged table, row by row. A conflicting cell holds the value and \
                the comment of the base version. A table of more than 200 rows has its first 200 rows here""")
        @JsonInclude(JsonInclude.Include.ALWAYS)
        List<List<RawTableCell>> cells,

        @Parameter(description = """
                The Excel styles the cells of the merged table and of its conflicts use, each once: a \
                cell and a piece of its text name their style by its index in this list""")
        @JsonInclude(JsonInclude.Include.ALWAYS)
        List<RawTableCellStyle> styles,

        @Parameter(description = "How many rows the merged table has")
        int rows,

        @Parameter(description = """
                The cells of the rows shown whose value, formula or comment one branch alone changed, which \
                the merge takes from that branch by itself, in A1 notation, the cells of a row or a column one branch \
                alone has included""")
        @JsonInclude(JsonInclude.Include.ALWAYS)
        List<String> changedCells,

        @Parameter(description = """
                The cells both branches changed differently, row by row, each as it is with your and \
                with their version""")
        @JsonInclude(JsonInclude.Include.ALWAYS)
        List<ConflictCellView> conflicts
) {
}
