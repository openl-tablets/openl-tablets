package org.openl.studio.projects.model.tables;

import java.util.Collection;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import io.swagger.v3.oas.annotations.media.Schema;

import org.openl.util.CollectionUtils;

/**
 * Datatype table model
 *
 * @author Vladyslav Pikus
 */
@JsonDeserialize(builder = DatatypeView.Builder.class)
public class DatatypeView extends TableView implements EditableTableView {

    private static final int BASE_BODY_WIDTH = 3;

    public static final String TABLE_TYPE = "Datatype";

    @Schema(description = "Name of the parent datatype to extend (inheritance)")
    @JsonProperty("extends")
    public final String extendz;

    @Schema(description = "Collection of fields defined in this datatype")
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

    @Override
    protected int getBodyHeight() {
        return CollectionUtils.isNotEmpty(fields) ? fields.size() : 0;
    }

    @Override
    protected int getBodyWidth() {
        return BASE_BODY_WIDTH;
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
