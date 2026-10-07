package org.openl.studio.projects.model.tables;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Data table model for getting and updating Data tables
 *
 * @author Vladyslav Pikus
 */
@JsonDeserialize(builder = DataView.Builder.class)
public class DataView extends AbstractDataView implements EditableTableView {

    public static final String TABLE_TYPE = "Data";

    @Schema(description = "Data type of the data table")
    public final String dataType;

    private DataView(Builder builder) {
        super(builder);
        this.dataType = builder.dataType;
    }

    @Override
    public String getTableType() {
        return tableType;
    }

    @JsonCreator
    public static Builder builder() {
        return new Builder();
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder extends AbstractDataView.Builder<Builder> {
        private String dataType;

        private Builder() {
            tableType(TABLE_TYPE);
        }

        public Builder dataType(String dataType) {
            this.dataType = dataType;
            return this;
        }

        @Override
        protected Builder self() {
            return this;
        }

        @Override
        public DataView build() {
            return new DataView(this);
        }
    }

}
