package org.openl.rules.webstudio.projects.tests.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;

public class TestParameterValue {

    @Parameter(description = "Name of the parameter")
    private final String name;

    @Parameter(description = "Description of the parameter")
    private final String description;

    @Schema(description = "JSON value of the parameter", implementation = Object.class)
    private final JsonNode value;

    @Schema(description = "JSON Schema of the parameter value", implementation = Object.class)
    private final ObjectNode schema;

    private TestParameterValue(Builder builder) {
        this.name = builder.name;
        this.description = builder.description;
        this.value = builder.value;
        this.schema = builder.schema;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public JsonNode getValue() {
        return value;
    }

    public ObjectNode getSchema() {
        return schema;
    }

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
            return new TestParameterValue(this);
        }
    }
}
