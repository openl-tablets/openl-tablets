package org.openl.studio.projects.model.run;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import org.openl.rules.rest.compile.MessageDescription;
import org.openl.studio.projects.model.ParameterValue;

@Builder
@Schema(description = "Run execution result containing method output, parameters, and metadata")
public record RunExecutionResult(
        @Parameter(description = "Table name")
        String tableName,

        @Parameter(description = "Table ID")
        String tableId,

        @Parameter(description = "Execution time in milliseconds")
        double executionTimeMs,

        @Schema(description = "Execution result as JSON", implementation = Object.class)
        JsonNode result,

        @Schema(description = "JSON Schema of the result type", implementation = Object.class)
        ObjectNode resultSchema,

        @Parameter(description = "Input parameters")
        List<ParameterValue> parameters,

        @Parameter(description = "Runtime context parameters")
        List<ParameterValue> contextParameters,

        @Parameter(description = "Execution errors")
        List<MessageDescription> errors
) {
}
