package org.openl.rules.rest.model.tables;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

/**
 * Cell view model for spreadsheet tables
 *
 * @author Vladyslav Pikus
 */
@JsonDeserialize(builder = SpreadsheetCellView.Builder.class)
public class SpreadsheetCellView {

    public final Object value;

    private SpreadsheetCellView(Builder builder) {
        this.value = builder.value;
    }

    @JsonCreator
    public static Builder builder() {
        return new Builder();
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
        private Object value;

        private Builder() {
        }

        public Builder value(Object value) {
            this.value = value;
            return this;
        }

        public SpreadsheetCellView build() {
            return new SpreadsheetCellView(this);
        }
    }

}
