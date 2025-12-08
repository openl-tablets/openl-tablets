package org.openl.studio.projects.model.tables;

import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import io.swagger.v3.oas.annotations.media.Schema;

import org.openl.util.CollectionUtils;

/**
 * {@code SimpleSpreadsheet} table model
 *
 * @author Vladyslav Pikus
 */
@JsonDeserialize(builder = SimpleSpreadsheetView.Builder.class)
public class SimpleSpreadsheetView extends ExecutableView {

    private static final int DEFAULT_WIDTH = 2;

    public static final String TABLE_TYPE = "SimpleSpreadsheet";

    @Schema(description = "Collection of spreadsheet steps/rows")
    public final List<SpreadsheetStepView> steps;

    private SimpleSpreadsheetView(Builder builder) {
        super(builder);
        this.steps = Optional.ofNullable(builder.steps).map(List::copyOf).orElseGet(List::of);
    }

    @Override
    protected int getBodyHeight() {
        var stepsCount = CollectionUtils.isNotEmpty(steps) ? steps.size() : 0;
        return stepsCount + SpreadsheetView.RESERVED_HEADER_HEIGHT;
    }

    @Override
    protected int getBodyWidth() {
        return DEFAULT_WIDTH;
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
