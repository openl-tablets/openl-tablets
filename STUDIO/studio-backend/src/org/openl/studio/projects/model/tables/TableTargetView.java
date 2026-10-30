package org.openl.studio.projects.model.tables;

import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import org.jspecify.annotations.Nullable;

/**
 * A table that a test or a run table exercises.
 *
 * <p>The table being tested is written where its author put it — another module of this project, or a module of a
 * project this one depends on — so it says where it lives and a screen can open it there.
 *
 * <p>A test written against a table that has several versions exercises every one of them. Each is answered with
 * the values of the dimension properties that tell it from the others, which a screen writes in the formats of its
 * reader.
 *
 * @param id         identifier of the tested table, the one the Tables API addresses it by
 * @param name       name the tested table is known by
 * @param dimensions values of the dimension properties that tell this version from the others, absent for a table
 *                   written once
 * @param module     module the tested table is written in
 * @param project    name of the project that module belongs to
 * @param projectId  identifier that project is addressed by, absent when the session cannot address it
 * @author Vladyslav Pikus
 */
@Builder
@Schema(description = "A table that a test or run table exercises")
public record TableTargetView(

        @Schema(description = "Identifier of the tested table, the one the Tables API addresses it by")
        String id,

        @Schema(description = "Name the tested table is known by")
        String name,

        @Schema(description = """
                Values of the dimension properties that tell this version of the tested table from \
                the others, by the technical name of the property. A date is written with no time zone. Absent \
                unless the table is written in more than one version""")
        @Nullable Map<String, Object> dimensions,

        @Schema(description = "Module the tested table is written in, which need not be the one being read")
        String module,

        @Schema(description = "Name of the project the tested table belongs to")
        String project,

        @Schema(description = "Identifier of that project, as the Projects API addresses it")
        String projectId) {
}
