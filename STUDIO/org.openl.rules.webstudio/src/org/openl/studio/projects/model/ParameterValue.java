package org.openl.studio.projects.model;

import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import org.openl.studio.common.model.GenericView;

/**
 * Represents a parameter value with optional lazy loading support.
 * <p>
 * Used across run, test, and trace APIs. The {@code lazy} and {@code parameterId}
 * fields are only relevant for trace and are annotated with
 * {@link JsonView @JsonView(Full)} to exclude them from non-trace OpenAPI schemas.
 * </p>
 */
@Builder
@Schema(description = "trace.type.parameter-value.desc")
public record ParameterValue(
        @Schema(description = "trace.field.param.name.desc")
        String name,

        @Schema(description = "trace.field.param.description.desc")
        String description,

        @Schema(description = "trace.field.param.lazy.desc")
        @JsonView(GenericView.Full.class)
        Boolean lazy,

        @Schema(description = "trace.field.param.parameter-id.desc")
        @JsonView(GenericView.Full.class)
        Integer parameterId,

        @Schema(description = "trace.field.param.value.desc", implementation = Object.class)
        JsonNode value,

        @Schema(description = "trace.field.param.schema.desc", implementation = Object.class)
        ObjectNode schema
) {
}
