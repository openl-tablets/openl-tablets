package org.openl.studio.projects.model.trace;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Parameter value for trace node with lazy loading support.
 */
@Schema(description = "Parameter value for trace node")
public record TraceParameterValue(
        @Schema(description = "Parameter name")
        String name,

        @Schema(description = "Parameter description")
        String description,

        @Schema(description = "Whether value is lazy-loaded (not included in initial response)")
        boolean lazy,

        @Schema(description = "Unique ID for lazy loading, null if value is included")
        Integer parameterId,

        @Schema(description = "JSON value, null if lazy=true", implementation = Object.class)
        JsonNode value,

        @Schema(description = "JSON Schema for UI tree building", implementation = Object.class)
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
