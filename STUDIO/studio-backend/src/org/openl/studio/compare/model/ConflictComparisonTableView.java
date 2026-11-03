package org.openl.studio.compare.model;

import java.util.List;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import org.jspecify.annotations.Nullable;

/**
 * A table as the base version and the two versions of a conflict hold it, the rows and the columns of the three lined
 * up by the lines of the base.
 *
 * @param id           identifier of the table within its comparison
 * @param name         the header of the table
 * @param status       what the two versions did to the table together
 * @param yoursStatus  what your version did to the table
 * @param theirsStatus what their version did to the table
 * @param baseRange    the place of the table in the base
 * @param yoursRange   the place of the table in your version
 * @param theirsRange  the place of the table in their version
 * @param base         the table as the base holds it
 * @param yours        the table as your version holds it, with the cells it changed
 * @param theirs       the table as their version holds it, with the cells it changed
 * @param rows         the rows, lined up
 * @param columns      the columns, lined up
 */
@Builder
@Schema(description = "A table as the base version and the two versions of a conflict hold it")
public record ConflictComparisonTableView(

        @Schema(description = "Identifier of the table within its comparison")
        String id,

        @Schema(description = "The header of the table")
        String name,

        @Parameter(description = "What the two versions did to the table together: `equal` when neither changed it")
        ComparisonNodeStatus status,

        @Parameter(description = "What your version did to the table")
        ComparisonNodeStatus yoursStatus,

        @Parameter(description = "What their version did to the table")
        ComparisonNodeStatus theirsStatus,

        @Parameter(description = "The place of the table in the base, in A1 notation; absent for a table a version added")
        @Nullable String baseRange,

        @Parameter(description = "The place of the table in your version; absent when it does not hold the table")
        @Nullable String yoursRange,

        @Parameter(description = "The place of the table in their version; absent when it does not hold the table")
        @Nullable String theirsRange,

        @Parameter(description = "The table as the base holds it; absent for a table a version added")
        @Nullable ComparisonSideView base,

        @Parameter(description = """
                The table as your version holds it, with the cells it changed against the base and the cells both \
                versions changed differently; absent when it does not hold the table""")
        @Nullable ComparisonSideView yours,

        @Parameter(description = """
                The table as their version holds it, with the cells it changed against the base and the cells both \
                versions changed differently; absent when it does not hold the table""")
        @Nullable ComparisonSideView theirs,

        @Parameter(description = """
                The rows, lined up by the rows of the base: before each row of the base, the rows your version and \
                then their version inserted there""")
        List<ConflictComparisonLineView> rows,

        @Parameter(description = "The columns, lined up the same way")
        List<ConflictComparisonLineView> columns) {
}
