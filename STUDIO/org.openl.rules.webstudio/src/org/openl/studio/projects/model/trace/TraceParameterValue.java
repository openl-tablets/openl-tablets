package org.openl.studio.projects.model.trace;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Parameter value for trace node with lazy loading support.
 */
@Schema(description = "trace.field.param.name.desc")
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

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String name;
        private String description;
        private boolean lazy;
        private Integer parameterId;
        private JsonNode value;
        private ObjectNode schema;

        private Builder() {
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder lazy(boolean lazy) {
            this.lazy = lazy;
            return this;
        }

        public Builder parameterId(Integer parameterId) {
            this.parameterId = parameterId;
            return this;
        }

        public Builder value(JsonNode value) {
            this.value = value;
            return this;
        }

        public Builder schema(ObjectNode schema) {
            this.schema = schema;
            return this;
        }

        public TraceParameterValue build() {
            return new TraceParameterValue(name, description, lazy, parameterId, value, schema);
        }
    }
}
