package org.openl.studio.projects.model.tables;

import java.util.Collection;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Row view model for Data tables
 *
 * @author Vladyslav Pikus
 */
@JsonDeserialize(builder = DataRowView.Builder.class)
public class DataRowView {

    @Schema(description = "Collection of values in the data row")
    public final Collection<Object> values;

    private DataRowView(Builder builder) {
        this.values = builder.values;
    }

    @JsonCreator
    public static Builder builder() {
        return new Builder();
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
        private Collection<Object> values;

        private Builder() {
        }

        public Builder values(Collection<Object> values) {
            this.values = values;
            return this;
        }

        public DataRowView build() {
            return new DataRowView(this);
        }
    }

}
