package org.openl.studio.compare.model;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

/**
 * A row or a column of a compared table, as the two files hold it.
 *
 * @param first  the line in the first file, counted from the first line of the table from 0
 * @param second the line in the second file, counted the same way
 * @param status what became of the line
 */
@Schema(description = "A row or a column of a compared table, as the two files hold it")
public record ComparisonLineView(

        @Parameter(description = """
                The line in the first file, counted from the first row or column of the table from 0; absent for a \
                line only the second file holds""")
        @Nullable Integer first,

        @Parameter(description = """
                The line in the second file, counted from the first row or column of the table from 0; absent for a \
                line only the first file holds""")
        @Nullable Integer second,

        @Parameter(description = """
                What became of the line: `equal` or `changed` for a line both files hold, `added` for a line only the \
                second file holds, `removed` for a line only the first file holds""")
        ComparisonNodeStatus status) {
}
