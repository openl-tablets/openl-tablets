package org.openl.rules.rest.model.tables;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

/**
 * Header model for {@link SimpleRulesView} table
 *
 * @author Vladyslav Pikus
 */
@JsonDeserialize(builder = SimpleRuleHeaderView.Builder.class)
public class SimpleRuleHeaderView extends ARuleHeaderView {

    private SimpleRuleHeaderView(Builder builder) {
        super(builder);
    }

    @JsonCreator
    public static Builder builder() {
        return new Builder();
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder extends ARuleHeaderView.Builder<Builder> {

        private Builder() {
        }

        @Override
        protected Builder self() {
            return this;
        }

        @Override
        public SimpleRuleHeaderView build() {
            return new SimpleRuleHeaderView(this);
        }
    }

}
