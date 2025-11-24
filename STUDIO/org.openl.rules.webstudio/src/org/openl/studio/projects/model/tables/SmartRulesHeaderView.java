package org.openl.studio.projects.model.tables;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

/**
 * Header model for {@link SmartRulesView} table
 *
 * @author Vladyslav Pikus
 */
@JsonDeserialize(builder = SmartRulesHeaderView.Builder.class)
public class SmartRulesHeaderView extends ARuleHeaderView {

    public transient int width;

    private SmartRulesHeaderView(Builder builder) {
        super(builder);
        this.width = builder.width;
    }

    @JsonCreator
    public static Builder builder() {
        return new Builder();
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder extends ARuleHeaderView.Builder<Builder> {

        public int width;

        private Builder() {
            width(1);
        }

        public Builder width(int width) {
            this.width = width;
            return this;
        }

        @Override
        protected Builder self() {
            return this;
        }

        @Override
        public SmartRulesHeaderView build() {
            return new SmartRulesHeaderView(this);
        }
    }

}
