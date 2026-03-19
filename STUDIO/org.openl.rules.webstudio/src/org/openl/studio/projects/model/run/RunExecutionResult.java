package org.openl.studio.projects.model.run;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.v3.oas.annotations.media.Schema;

import org.openl.rules.rest.compile.MessageDescription;

@Schema(description = "Run execution result containing method output, parameters, and metadata")
public record RunExecutionResult(
        @Schema(description = "Table name")
        String tableName,

        @Schema(description = "Table ID")
        String tableId,

        @Schema(description = "Execution time in milliseconds")
        double executionTimeMs,

        @Schema(description = "Execution result as JSON", implementation = Object.class)
        JsonNode result,

        @Schema(description = "JSON Schema of the result type", implementation = Object.class)
        ObjectNode resultSchema,

        @Schema(description = "Input parameters")
        List<RunParameterValue> parameters,

        @Schema(description = "Runtime context parameters")
        List<RunParameterValue> contextParameters,

        @Schema(description = "Execution errors")
        List<MessageDescription> errors
) {
}
