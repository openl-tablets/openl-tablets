package org.openl.studio.projects.model.tables;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

/**
 * A table that a test or a run table exercises.
 *
 * <p>The table being tested is written where its author put it — another module of this project, or a module of a
 * project this one depends on — so it says where it lives and a screen can open it there.
 *
 * <p>A test written against a table that has several versions exercises every one of them, and each is answered
 * here under the name that tells it from the others.
 *
 * @param id        identifier of the tested table, the one the Tables API addresses it by
 * @param name      name the tested table is known by, with the dimension properties that tell its version apart
 * @param module    module the tested table is written in
 * @param project   name of the project that module belongs to
 * @param projectId identifier that project is addressed by, absent when the session cannot address it
 * @author Vladyslav Pikus
 */
@Builder
@Schema(description = "A table that a test or run table exercises")
public record TableTargetView(

        @Schema(description = "Identifier of the tested table, the one the Tables API addresses it by")
        String id,

        @Schema(description = "Name the tested table is known by, with the dimension properties that tell this "
                + "version of it from the others")
        String name,

        @Schema(description = "Module the tested table is written in, which need not be the one being read")
        String module,

        @Schema(description = "Name of the project the tested table belongs to")
        String project,

        @Schema(description = "Identifier of that project, as the Projects API addresses it")
        String projectId) {
}
