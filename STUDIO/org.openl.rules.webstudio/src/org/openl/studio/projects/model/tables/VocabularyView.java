package org.openl.studio.projects.model.tables;

import java.util.Collection;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import io.swagger.v3.oas.annotations.media.Schema;

import org.openl.util.CollectionUtils;

/**
 * {@code Vocabulary} table model
 *
 * @author Vladyslav Pikus
 */
@JsonDeserialize(builder = VocabularyView.Builder.class)
public class VocabularyView extends TableView implements EditableTableView {

    private static final int BASE_BODY_WIDTH = 1;

    public static final String TABLE_TYPE = "Vocabulary";

    @Schema(description = "Type of vocabulary values (e.g., String, Integer, etc.)")
    public final String type;

    @Schema(description = "Collection of vocabulary values")
    public final Collection<VocabularyValueView> values;

    private VocabularyView(Builder builder) {
        super(builder);
        this.type = builder.type;
        this.values = builder.values;
    }

    @Override
    protected int getBodyHeight() {
        return CollectionUtils.isNotEmpty(values) ? values.size() : 0;
    }

    @Override
    protected int getBodyWidth() {
        return BASE_BODY_WIDTH;
    }

    @JsonCreator
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String getTableType() {
        return tableType;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder extends TableView.Builder<Builder> {
        private String type;
        private Collection<VocabularyValueView> values;

        private Builder() {
            tableType(TABLE_TYPE);
        }

        public Builder type(String type) {
            this.type = type;
            return this;
        }

        public Builder values(Collection<VocabularyValueView> values) {
            this.values = values;
            return this;
        }

        @Override
        protected Builder self() {
            return this;
        }

        @Override
        public VocabularyView build() {
            return new VocabularyView(this);
        }
    }

}
