package org.openl.studio.projects.model.tables;

import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * {@code SimpleSpreadsheet} table model
 *
 * @author Vladyslav Pikus
 */
@JsonDeserialize(builder = SimpleSpreadsheetView.Builder.class)
public class SimpleSpreadsheetView extends ExecutableView {

    public static final String TABLE_TYPE = "SimpleSpreadsheet";

    @Schema(description = "Collection of spreadsheet steps/rows")
    public final List<SpreadsheetStepView> steps;

    private SimpleSpreadsheetView(Builder builder) {
        super(builder);
        this.steps = Optional.ofNullable(builder.steps).map(List::copyOf).orElseGet(List::of);
    }

    @JsonCreator
    public static Builder builder() {
        return new Builder();
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder extends ExecutableView.Builder<Builder> {
        private List<SpreadsheetStepView> steps;

        private Builder() {
            tableType(TABLE_TYPE);
        }

        @Override
        protected Builder self() {
            return this;
        }

        public Builder steps(List<SpreadsheetStepView> steps) {
            this.steps = steps;
            return this;
        }

        public SimpleSpreadsheetView build() {
            return new SimpleSpreadsheetView(this);
        }
    }
}
