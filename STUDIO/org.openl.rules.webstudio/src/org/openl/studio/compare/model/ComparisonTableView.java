package org.openl.studio.compare.model;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import org.jspecify.annotations.Nullable;

/**
 * A table of the comparison, as it stands in each of the two files.
 *
 * <p>A table that only one file holds has that side alone; the other one is absent.
 *
 * @param id     identifier of the table within its comparison
 * @param name   name of the table as it reads in the file it comes from
 * @param status how the table of the second file relates to the first one
 * @param first  the table as it stands in the first file
 * @param second the table as it stands in the second file
 */
@Builder
public record ComparisonTableView(

        @Schema(description = "Identifier of the table within its comparison")
        String id,

        @Schema(description = "Name of the table as it reads in the file it comes from")
        String name,

        @Parameter(description = "How the table of the second file relates to the first one")
        ComparisonNodeStatus status,

        @Parameter(description = "The table as it stands in the first file; absent when only the second file holds it")
        @Nullable ComparisonSideView first,

        @Parameter(description = "The table as it stands in the second file; absent when only the first file holds it")
        @Nullable ComparisonSideView second) {
}
