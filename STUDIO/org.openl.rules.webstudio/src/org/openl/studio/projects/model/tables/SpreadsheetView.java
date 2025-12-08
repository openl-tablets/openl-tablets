package org.openl.studio.projects.model.tables;

import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import io.swagger.v3.oas.annotations.media.Schema;

import org.openl.util.CollectionUtils;

/**
 * Spreadsheet table model
 *
 * @author Vladyslav Pikus
 */
@JsonDeserialize(builder = SpreadsheetView.Builder.class)
public class SpreadsheetView extends ExecutableView {

    protected static final int RESERVED_HEADER_HEIGHT = 1;
    private static final int RESERVED_STEPS_WIDTH = 1;

    public static final String TABLE_TYPE = "Spreadsheet";

    @Schema(description = "Collection of spreadsheet rows")
    public final List<SpreadsheetRowView> rows;

    @Schema(description = "Collection of spreadsheet columns")
    public final List<SpreadsheetColumnView> columns;

    @Schema(description = "2D array of spreadsheet cells")
    public final SpreadsheetCellView[][] cells;

    private SpreadsheetView(Builder builder) {
        super(builder);
        this.rows = Optional.ofNullable(builder.rows).map(List::copyOf).orElse(List.of());
        this.columns = Optional.ofNullable(builder.columns).map(List::copyOf).orElse(List.of());
        this.cells = builder.cells;
    }

    @Override
    protected int getBodyHeight() {
        var stepsCount = CollectionUtils.isNotEmpty(rows) ? rows.size() : 0;
        return stepsCount + RESERVED_HEADER_HEIGHT;
    }

    @Override
    protected int getBodyWidth() {
        var colCount = CollectionUtils.isNotEmpty(rows) ? rows.size() : 0;
        return colCount + RESERVED_STEPS_WIDTH;
    }

    @JsonCreator
    public static Builder builder() {
        return new Builder();
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder extends ExecutableView.Builder<Builder> {
        private List<SpreadsheetRowView> rows;
        private List<SpreadsheetColumnView> columns;
        private SpreadsheetCellView[][] cells;

        private Builder() {
            tableType(TABLE_TYPE);
        }

        @Override
        protected Builder self() {
            return this;
        }

        public Builder rows(List<SpreadsheetRowView> rows) {
            this.rows = rows;
            return this;
        }

        public Builder columns(List<SpreadsheetColumnView> columns) {
            this.columns = columns;
            return this;
        }

        public Builder cells(SpreadsheetCellView[][] cells) {
            this.cells = cells;
            return this;
        }

        public SpreadsheetView build() {
            return new SpreadsheetView(this);
        }
    }

}
