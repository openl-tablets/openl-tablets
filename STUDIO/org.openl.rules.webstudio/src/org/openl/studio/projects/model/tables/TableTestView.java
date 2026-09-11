package org.openl.studio.projects.model.tables;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

/**
 * A test or run table that exercises another table.
 *
 * @param id   identifier of the test table itself, so it can be opened like any other
 * @param name name the test is known by
 * @param info what the test holds, as the Editor phrases it — "1 test case"; absent for a run table, which holds
 *             no cases
 * @author Vladyslav Pikus
 */
@Builder
@Schema(description = "A test or run table that exercises another table")
public record TableTestView(

        @Schema(description = "Identifier of the test table, the one the Tables API addresses it by")
        String id,

        @Schema(description = "Name the test is known by")
        String name,

        @Schema(description = "What the test holds, for example '1 test case'. Absent for a run table.")
        String info) {
}
