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
 * @param firstRange  the place of the table in the first file
 * @param secondRange the place of the table in the second file
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
                The place of the table in the first file, in A1 notation; absent for a sheet and for a table only \
                the second file holds""")
        @Nullable String firstRange,

        @Parameter(description = """
                The place of the table in the second file, in A1 notation; absent for a sheet and for a table only \
                the first file holds""")
        @Nullable String secondRange,

        @Schema(description = "Tables of the sheet; empty for a table")
        List<ComparisonNodeView> children) {
}
