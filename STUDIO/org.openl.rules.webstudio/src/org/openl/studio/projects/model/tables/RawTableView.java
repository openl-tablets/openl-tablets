package org.openl.studio.projects.model.tables;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Represents a table in raw format as a 2D matrix of cells without any parsing or structure interpretation.
 * <p>
 * Unlike parsed APIs (DataView, TestView), RawTableView:
 * <ul>
 *   <li>Does NOT parse table headers or extract field names
 *   <li>Does NOT determine data types
 *   <li>Does NOT validate table structure
 *   <li>Returns the exact table content as a 2D matrix with merge information
 *   <li>Includes all rows starting from row 0 (header row included)
 * </ul>
 * <p>
 * The {@code source} field contains the entire table as a 2D matrix of {@link RawTableCell} objects.
 * Each cell includes explicit colspan/rowspan information and a covered flag for masked cells.
 * <p>
 * Use Cases:
 * <ul>
 *   <li>Exporting tables in their original structure
 *   <li>Reading tables of unknown or custom types
 *   <li>Programmatic access to cell-level data with merge information
 *   <li>Preserving exact cell positioning and merge regions
 * </ul>
 * <p>
 * Access via REST API: {@code GET /projects/{projectId}/tables/{tableId}?raw=true}
 *
 * @author Vladyslav Pikus
 */
@JsonDeserialize(builder = RawTableView.Builder.class)
@JsonIgnoreProperties("properties")
public class RawTableView extends TableView implements EditableTableView {

    public static final String TABLE_TYPE = "RawSource";

    @Schema(description = "2D matrix of raw table cells with merge information")
    public final List<List<RawTableCell>> source;   // 2D matrix of cells

    private RawTableView(Builder builder) {
        super(builder);
        this.source = builder.source;
    }

    @Override
    public int getHeight() {
        return getBodyHeight();
    }

    @Override
    protected int getBodyHeight() {
        return source.size();
    }

    @Override
    public int getWidth() {
        return getBodyWidth();
    }

    @Override
    protected int getBodyWidth() {
        return source.stream()
                .mapToInt(List::size)
                .max()
                .orElse(1);
    }

    @Override
    public String getTableType() {
        return tableType;
    }

    public static Builder builder() {
        return new Builder();
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder extends TableView.Builder<Builder> {
        private List<List<RawTableCell>> source;

        private Builder() {
            tableType(TABLE_TYPE);
        }

        public Builder source(List<List<RawTableCell>> source) {
            this.source = source;
            return this;
        }

        @Override
        public Builder properties(Map<String, Object> properties) {
            // Ignore properties for RawTableView as they are part of the raw source
            return this;
        }

        @Override
        protected Builder self() {
            return this;
        }

        @Override
        public RawTableView build() {
            return new RawTableView(this);
        }
    }

}
