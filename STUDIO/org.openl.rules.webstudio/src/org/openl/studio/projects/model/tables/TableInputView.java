package org.openl.studio.projects.model.tables;

import java.util.List;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import org.jspecify.annotations.Nullable;

import org.openl.studio.projects.model.ParameterValue;

/**
 * The input a table takes to be executed.
 *
 * <p>A rule table is described by its declared parameters. Each parameter carries its name, its type, the JSON
 * schema of the values it accepts and the value it starts with. The runtime context is described the same way
 * when the project provides one to its rules.
 *
 * <p>A test table declares no parameters of its own. Its input is the cases it carries, which are read a page at
 * a time from the cases sub-resource.
 *
 * @param tableId        id of the table
 * @param name           name of the table
 * @param testTable      whether the table is a test table that carries its own cases
 * @param parameters     declared input parameters of a rule table, empty for a test table
 * @param runtimeContext schema of the runtime context, absent when the project provides none
 */
@Builder
@Schema(description = "The input a table takes to be executed")
public record TableInputView(
        @Parameter(description = "Id of the table")
        String tableId,

        @Parameter(description = "Name of the table")
        String name,

        @Parameter(description = "Whether the table is a test table that carries its own cases")
        boolean testTable,

        @Parameter(description = """
                Declared input parameters of a rule table; each carries the JSON schema of the values it \
                accepts and the value it starts with - the defaults its datatype declares, absent when it \
                starts unset. Empty for a test table""")
        List<ParameterValue> parameters,

        @Parameter(description = """
                Schema of the runtime context the rules receive, absent when the project provides none""")
        @Nullable ParameterValue runtimeContext
) {
}
