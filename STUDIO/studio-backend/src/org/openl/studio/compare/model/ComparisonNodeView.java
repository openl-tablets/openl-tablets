package org.openl.studio.compare.model;

import java.util.List;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import org.jspecify.annotations.Nullable;

/**
 * An element of the comparison tree: a sheet, or a table of a sheet.
 *
 * @param id          identifier of the element within its comparison
 * @param name        name of the element as it reads in the file it comes from
 * @param type        kind of the element
 * @param status      how the element of the second file relates to the first one
 * @param moved       whether the second file moved the table to another place on purpose
 * @param formattingOnly whether the formatting is all that differs in the table
 * @param firstRange  the place of the table in the first file
 * @param secondRange the place of the table in the second file
 * @param yours       what your version did to the element, in a comparison of a conflict with its base
 * @param theirs      what their version did to the element, in a comparison of a conflict with its base
 * @param other       whether the table holds cells outside OpenL tables, such as notes or formulas beside them
 * @param conflict    whether both versions of a conflict changed cells of the table differently
 * @param children    tables of a sheet; empty for a table
 */
@Builder
public record ComparisonNodeView(

        @Schema(description = """
                Identifier of the element within its comparison. A table is addressed by it to read \
                the two sides of the comparison.""")
        String id,

        @Schema(description = "Name of the element as it reads in the file it comes from")
        String name,

        @Parameter(description = "Kind of the element")
        ComparisonNodeType type,

        @Parameter(description = "How the element of the second file relates to the first one")
        ComparisonNodeStatus status,

        @Parameter(description = """
                Whether the second file moved the table to another place of its sheet on purpose, whatever else \
                changed. A table only pushed by the rows inserted or deleted above it is not moved. Absent for a \
                sheet.""")
        @Nullable Boolean moved,

        @Parameter(description = """
                True for a table that differs in its formatting only: no cell of it reads differently, and neither \
                file holds a row or a column the other does not. A reader may list it only when asked for, as an \
                equal table, or as a moved one when `moved` is true. Absent for any other table and for a sheet.""")
        @Nullable Boolean formattingOnly,

        @Parameter(description = """
                The place of the table in the first file, in A1 notation; absent for a sheet and for a table only \
                the second file holds""")
        @Nullable String firstRange,

        @Parameter(description = """
                The place of the table in the second file, in A1 notation; absent for a sheet and for a table only \
                the first file holds""")
        @Nullable String secondRange,

        @Parameter(description = """
                What your version did to the element, against the base version both versions started from; \
                present only in a comparison of a conflicted file with its base""")
        @Nullable ComparisonNodeStatus yours,

        @Parameter(description = """
                What their version did to the element, against the base version both versions started from; \
                present only in a comparison of a conflicted file with its base""")
        @Nullable ComparisonNodeStatus theirs,

        @Parameter(description = """
                True for a table of cells outside OpenL tables, such as notes or formulas beside them, which a \
                reader lists only when asked for; absent for an OpenL table and for a sheet""")
        @Nullable Boolean other,

        @Parameter(description = """
                True when both versions changed cells of the table differently; present only in a comparison of \
                a conflicted file with its base""")
        @Nullable Boolean conflict,

        @Schema(description = "Tables of the sheet; empty for a table")
        List<ComparisonNodeView> children) {
}
