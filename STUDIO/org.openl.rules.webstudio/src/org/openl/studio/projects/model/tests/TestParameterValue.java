package org.openl.studio.projects.model.tests;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;

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

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String name;
        private String description;
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

        public Builder value(JsonNode value) {
            this.value = value;
            return this;
        }

        public Builder schema(ObjectNode schema) {
            this.schema = schema;
            return this;
        }

        public TestParameterValue build() {
            return new TestParameterValue(name, description, value, schema);
        }
    }
}
