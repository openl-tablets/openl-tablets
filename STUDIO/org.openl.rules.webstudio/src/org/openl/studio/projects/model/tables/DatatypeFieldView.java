package org.openl.studio.projects.model.tables;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Field model for datatype tables
 *
 * @author Vladyslav Pikus
 */
@JsonDeserialize(builder = DatatypeFieldView.Builder.class)
public class DatatypeFieldView {

    @Schema(description = "Name of the datatype field")
    public final String name;

    @Schema(description = "Data type of the datatype field (e.g., String, Integer, etc.)")
    public final String type;

    @Schema(description = "Default value of the datatype field")
    public final Object defaultValue;

    @Schema(description = "Indicates whether the datatype field is required")
    public final String required;

    private DatatypeFieldView(Builder builder) {
        this.name = builder.name;
        this.type = builder.type;
        this.defaultValue = builder.defaultValue;
        this.required = builder.required;
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
        private String required;

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

        public Builder required(String required) {
            this.required = required;
            return this;
        }

        public DatatypeFieldView build() {
            return new DatatypeFieldView(this);
        }
    }

}
