package org.openl.studio.projects.model.tables;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

/**
 * A test or run table that exercises another table.
 *
 * <p>A test is a table of its own and is written where its author put it — another module of this project, or a
 * module of a project this one depends on. It therefore says where it lives, so a screen can open it there
 * rather than looking for it beside the table it exercises.
 *
 * @param id        identifier of the test table itself, so it can be opened like any other
 * @param name      name the test is known by
 * @param info      what the test holds, as the Editor phrases it — "1 test case"; absent for a run table, which
 *                  holds no cases
 * @param module    module the test is written in
 * @param project   name of the project that module belongs to
 * @param projectId identifier that project is addressed by, absent when the session cannot address it
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
        String info,

        @Schema(description = "Module the test table is written in, which need not be the one being read")
        String module,

        @Schema(description = "Name of the project the test table belongs to")
        String project,

        @Schema(description = "Identifier of that project, as the Projects API addresses it")
        String projectId) {
}
