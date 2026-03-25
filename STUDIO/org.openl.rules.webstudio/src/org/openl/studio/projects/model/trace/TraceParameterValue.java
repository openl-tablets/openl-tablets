package org.openl.studio.projects.model.trace;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

/**
 * Parameter value for trace node with lazy loading support.
 */
@Builder
@Schema(description = "trace.type.parameter-value.desc")
public record TraceParameterValue(
        @Schema(description = "trace.field.param.name.desc")
        String name,

        @Schema(description = "trace.field.param.description.desc")
        String description,

        @Schema(description = "trace.field.param.lazy.desc")
        boolean lazy,

        @Schema(description = "trace.field.param.parameter-id.desc")
        Integer parameterId,

        @Schema(description = "trace.field.param.value.desc", implementation = Object.class)
        JsonNode value,

        @Schema(description = "trace.field.param.schema.desc", implementation = Object.class)
        ObjectNode schema
) {
}
