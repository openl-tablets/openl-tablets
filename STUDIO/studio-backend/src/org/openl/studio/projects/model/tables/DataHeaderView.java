package org.openl.studio.projects.model.tables;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Header definition for Data table column
 *
 * @author Vladyslav Pikus
 */
@JsonDeserialize(builder = DataHeaderView.Builder.class)
public class DataHeaderView {


    @Schema(description = "Name of the field in the data type that this column represents")
    public final String fieldName;

    @Schema(description = "Foreign key reference if this field is a foreign key")
    public final String foreignKey;

    @Schema(description = "Display name of the column header")
    public final String displayName;

    private DataHeaderView(Builder builder) {
        this.fieldName = builder.fieldName;
        this.foreignKey = builder.foreignKey;
        this.displayName = builder.displayName;
    }

    @JsonCreator
    public static Builder builder() {
        return new Builder();
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
        private String fieldName;
        private String foreignKey;
        private String displayName;

        private Builder() {
        }

        public Builder fieldName(String fieldName) {
            this.fieldName = fieldName;
            return this;
        }

        public Builder foreignKey(String foreignKey) {
            this.foreignKey = foreignKey;
            return this;
        }

        public Builder displayName(String displayName) {
            this.displayName = displayName;
            return this;
        }

        public DataHeaderView build() {
            return new DataHeaderView(this);
        }
    }

}
