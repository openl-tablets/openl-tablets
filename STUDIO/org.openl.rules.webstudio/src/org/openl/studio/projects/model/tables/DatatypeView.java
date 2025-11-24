package org.openl.studio.projects.model.tables;

import java.util.Collection;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

/**
 * Datatype table model
 *
 * @author Vladyslav Pikus
 */
@JsonDeserialize(builder = DatatypeView.Builder.class)
public class DatatypeView extends TableView implements EditableTableView {

    public static final String TABLE_TYPE = "Datatype";

    @JsonProperty("extends")
    public final String extendz;

    public final Collection<DatatypeFieldView> fields;

    private DatatypeView(Builder builder) {
        super(builder);
        this.extendz = builder.extendz;
        this.fields = builder.fields;
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
    public static class Builder extends TableView.Builder<Builder> {
        private String extendz;
        private Collection<DatatypeFieldView> fields;

        private Builder() {
            tableType(TABLE_TYPE);
        }

        @JsonProperty("extends")
        public Builder extendz(String extendz) {
            this.extendz = extendz;
            return this;
        }

        public Builder fields(Collection<DatatypeFieldView> fields) {
            this.fields = fields;
            return this;
        }

        @Override
        protected Builder self() {
            return this;
        }

        @Override
        public DatatypeView build() {
            return new DatatypeView(this);
        }
    }

}
