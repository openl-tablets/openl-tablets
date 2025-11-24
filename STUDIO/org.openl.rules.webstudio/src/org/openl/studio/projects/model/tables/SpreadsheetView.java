package org.openl.studio.projects.model.tables;

import java.util.Collection;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

/**
 * Spreadsheet table model
 *
 * @author Vladyslav Pikus
 */
@JsonDeserialize(builder = SpreadsheetView.Builder.class)
public class SpreadsheetView extends ExecutableView {

    public static final String TABLE_TYPE = "Spreadsheet";

    public final Collection<SpreadsheetRowView> rows;

    public final Collection<SpreadsheetColumnView> columns;

    public final SpreadsheetCellView[][] cells;

    private SpreadsheetView(Builder builder) {
        super(builder);
        this.rows = builder.rows;
        this.columns = builder.columns;
        this.cells = builder.cells;
    }

    @JsonCreator
    public static Builder builder() {
        return new Builder();
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder extends ExecutableView.Builder<Builder> {
        private Collection<SpreadsheetRowView> rows;
        private Collection<SpreadsheetColumnView> columns;
        private SpreadsheetCellView[][] cells;

        private Builder() {
            tableType(TABLE_TYPE);
        }

        @Override
        protected Builder self() {
            return this;
        }

        public Builder rows(Collection<SpreadsheetRowView> rows) {
            this.rows = rows;
            return this;
        }

        public Builder columns(Collection<SpreadsheetColumnView> columns) {
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
