package org.openl.rules.rest.model.tables;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

/**
 * Field model for datatype tables
 *
 * @author Vladyslav Pikus
 */
@JsonDeserialize(builder = DatatypeFieldView.Builder.class)
public class DatatypeFieldView {

    public final String name;
    public final String type;
    public final Object defaultValue;
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
