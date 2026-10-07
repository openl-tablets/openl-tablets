package org.openl.studio.projects.model.tables;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import io.swagger.v3.oas.annotations.Parameter;

/**
 * Field model for datatype tables
 *
 * @author Vladyslav Pikus
 */
@JsonDeserialize(builder = DatatypeFieldView.Builder.class)
public class DatatypeFieldView {

    @Parameter(description = "Name of the datatype field")
    public final String name;

    @Parameter(description = "Data type of the datatype field (e.g., String, Integer, etc.)")
    public final String type;

    @Parameter(description = "Default value of the datatype field")
    public final Object defaultValue;

    @Parameter(description = "Whether the datatype field must be filled in, from the Mandatory column")
    public final String mandatory;

    @Parameter(description = "Free-text description of the datatype field, from the Description column")
    public final String description;

    @Parameter(description = "Example value of the datatype field, from the Example column")
    public final Object example;

    private DatatypeFieldView(Builder builder) {
        this.name = builder.name;
        this.type = builder.type;
        this.defaultValue = builder.defaultValue;
        this.mandatory = builder.mandatory;
        this.description = builder.description;
        this.example = builder.example;
    }

    @JsonCreator
    public static Builder builder() {
        return new Builder();
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
        private String name;
        private String type;
        private Object defaultValue;
        private String mandatory;
        private String description;
        private Object example;

        private Builder() {
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder type(String type) {
            this.type = type;
            return this;
        }

        public Builder defaultValue(Object defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }

        public Builder mandatory(String mandatory) {
            this.mandatory = mandatory;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder example(Object example) {
            this.example = example;
            return this;
        }

        public DatatypeFieldView build() {
            return new DatatypeFieldView(this);
        }
    }

}
