package org.openl.studio.projects.model.tests;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record TestParameterValue(
        @Parameter(description = "Name of the parameter")
        String name,

        @Parameter(description = "Description of the parameter")
        String description,

        @Schema(description = "JSON value of the parameter", implementation = Object.class)
        JsonNode value,

        @Schema(description = "JSON Schema of the parameter value", implementation = Object.class)
        ObjectNode schema
) {
}
