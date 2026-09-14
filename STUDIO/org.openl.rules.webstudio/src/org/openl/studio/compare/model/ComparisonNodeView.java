package org.openl.studio.compare.model;

import java.util.List;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

/**
 * An element of the comparison tree: a sheet, or a table of a sheet.
 *
 * @param id       identifier of the element within its comparison
 * @param name     name of the element as it reads in the file it comes from
 * @param type     kind of the element
 * @param status   how the element of the second file relates to the first one
 * @param changes  properties that read differently in the two files
 * @param children tables of a sheet; empty for a table
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

        @Schema(description = "Properties that read differently in the two files; empty for a sheet")
        List<ComparisonPropertyChange> changes,

        @Schema(description = "Tables of the sheet; empty for a table")
        List<ComparisonNodeView> children) {
}
