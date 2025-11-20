package org.openl.studio.projects.model.tables;

import java.util.Collection;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

/**
 * {@code SimpleSpreadsheet} table model
 *
 * @author Vladyslav Pikus
 */
@JsonDeserialize(builder = SimpleSpreadsheetView.Builder.class)
public class SimpleSpreadsheetView extends ExecutableView {

    public static final String TABLE_TYPE = "SimpleSpreadsheet";

    public final Collection<SpreadsheetStepView> steps;

    private SimpleSpreadsheetView(Builder builder) {
        super(builder);
        this.steps = builder.steps;
    }

    @JsonCreator
    public static Builder builder() {
        return new Builder();
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder extends ExecutableView.Builder<Builder> {
        private Collection<SpreadsheetStepView> steps;

        private Builder() {
            tableType(TABLE_TYPE);
        }

        @Override
        protected Builder self() {
            return this;
        }

        public Builder steps(Collection<SpreadsheetStepView> steps) {
            this.steps = steps;
            return this;
        }

        public SimpleSpreadsheetView build() {
            return new SimpleSpreadsheetView(this);
        }
    }
}
