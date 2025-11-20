package org.openl.studio.projects.model.tables;

import java.util.Collection;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

/**
 * {@code Vocabulary} table model
 *
 * @author Vladyslav Pikus
 */
@JsonDeserialize(builder = VocabularyView.Builder.class)
public class VocabularyView extends TableView implements EditableTableView {

    public static final String TABLE_TYPE = "Vocabulary";

    public final String type;

    public final Collection<VocabularyValueView> values;

    private VocabularyView(Builder builder) {
        super(builder);
        this.type = builder.type;
        this.values = builder.values;
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
