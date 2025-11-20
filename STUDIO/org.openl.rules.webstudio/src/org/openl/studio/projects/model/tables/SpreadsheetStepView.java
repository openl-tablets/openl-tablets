package org.openl.studio.projects.model.tables;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

/**
 * Step view model for spreadsheet tables
 *
 * @author Vladyslav Pikus
 */
@JsonDeserialize(builder = SpreadsheetStepView.Builder.class)
public class SpreadsheetStepView {

    public final String name;
    public final String type;
    public final Object value;

    private SpreadsheetStepView(Builder builder) {
        this.name = builder.name;
        this.type = builder.type;
        this.value = builder.value;
    }

    @JsonCreator
    public static Builder builder() {
        return new Builder();
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
        private String name;
        private String type;
        private Object value;

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

        public Builder value(Object value) {
            this.value = value;
            return this;
        }

        public SpreadsheetStepView build() {
            return new SpreadsheetStepView(this);
        }
    }

}
