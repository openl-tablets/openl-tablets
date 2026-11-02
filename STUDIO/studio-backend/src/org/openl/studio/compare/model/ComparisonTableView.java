package org.openl.studio.compare.model;

import java.util.List;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import org.jspecify.annotations.Nullable;

/**
 * A table of the comparison, as it stands in each of the two files.
 *
 * <p>A table that only one file holds has that side alone. The other side is absent, and so are the lines.
 *
 * <p>The rows and the columns of a table both files hold are lined up the way a text comparison lines up lines, in
 * the order they are read: a removed line before the line added in its place.
 *
 * @param id                identifier of the table within its comparison
 * @param name              name of the table as it reads in the file it comes from
 * @param status            how the table of the second file relates to the first one
 * @param moved             whether the second file moved the table to another place on purpose
 * @param formattingChanged whether the formatting of a cell both files hold differs
 * @param firstRange        the place of the table in the first file
 * @param secondRange       the place of the table in the second file
 * @param first             the table as it stands in the first file
 * @param second            the table as it stands in the second file
 * @param rows              the rows of the table, lined up
 * @param columns           the columns of the table, lined up
 */
@Builder
public record ComparisonTableView(

        @Schema(description = "Identifier of the table within its comparison")
        String id,

        @Schema(description = "Name of the table as it reads in the file it comes from")
        String name,

        @Parameter(description = "How the table of the second file relates to the first one")
        ComparisonNodeStatus status,

        @Parameter(description = """
                Whether the second file moved the table to another place of its sheet on purpose, whatever else \
                changed""")
        boolean moved,

        @Parameter(description = """
                Whether the formatting of a cell both files hold differs. A change of formatting alone marks no \
                cell.""")
        boolean formattingChanged,

        @Parameter(description = """
                The place of the table in the first file, in A1 notation; absent when only the second file holds \
                it""")
        @Nullable String firstRange,

        @Parameter(description = """
                The place of the table in the second file, in A1 notation; absent when only the first file holds \
                it""")
        @Nullable String secondRange,

        @Parameter(description = "The table as it stands in the first file; absent when only the second file holds it")
        @Nullable ComparisonSideView first,

        @Parameter(description = "The table as it stands in the second file; absent when only the first file holds it")
        @Nullable ComparisonSideView second,

        @Parameter(description = """
                The rows of the table both files hold, lined up in the order they are read; empty for a table only \
                one file holds""")
        List<ComparisonLineView> rows,

        @Parameter(description = """
                The columns of the table both files hold, lined up in the order they are read; empty for a table \
                only one file holds""")
        List<ComparisonLineView> columns) {
}
