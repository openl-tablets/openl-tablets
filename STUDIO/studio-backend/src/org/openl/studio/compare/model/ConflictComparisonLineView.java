package org.openl.studio.compare.model;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

/**
 * A row or a column of a table, as the base version and the two versions of a conflict hold it.
 *
 * @param base         the line in the base, counted from the first line of the table from 0
 * @param yours        the line in your version, counted the same way
 * @param theirs       the line in their version, counted the same way
 * @param yoursStatus  what your version did to the line
 * @param theirsStatus what their version did to the line
 */
@Schema(description = "A row or a column of a table, as the base version and the two versions of a conflict hold it")
public record ConflictComparisonLineView(

        @Parameter(description = """
                The line in the base, counted from the first row or column of the table from 0; absent for a line \
                a version inserted""")
        @Nullable Integer base,

        @Parameter(description = "The line in your version, counted the same way; absent when it does not hold the line")
        @Nullable Integer yours,

        @Parameter(description = "The line in their version, counted the same way; absent when it does not hold the line")
        @Nullable Integer theirs,

        @Parameter(description = """
                What your version did to the line of the base: `equal`, `changed` or `removed`; `added` for a line \
                it inserted, and `equal` for a line their version inserted""")
        ComparisonNodeStatus yoursStatus,

        @Parameter(description = """
                What their version did to the line of the base: `equal`, `changed` or `removed`; `added` for a line \
                it inserted, and `equal` for a line your version inserted""")
        ComparisonNodeStatus theirsStatus) {
}
