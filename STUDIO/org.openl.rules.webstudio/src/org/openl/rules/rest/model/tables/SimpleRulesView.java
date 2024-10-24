package org.openl.rules.rest.model.tables;

import java.util.LinkedHashMap;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

/**
 * {@code SimpleRules} table model
 *
 * @author Vladyslav Pikus
 */
@JsonDeserialize(builder = SimpleRulesView.Builder.class)
public class SimpleRulesView extends ExecutableView {

    public static final String TABLE_TYPE = "SimpleRules";

    public final List<SimpleRuleHeaderView> headers;

    public final List<LinkedHashMap<String, Object>> rules;

    public SimpleRulesView(Builder builder) {
        super(builder);
        this.rules = builder.rules;
        this.headers = builder.headers;
    }

    @JsonCreator
    public static Builder builder() {
        return new Builder();
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder extends ExecutableView.Builder<Builder> {

        private List<SimpleRuleHeaderView> headers;
        private List<LinkedHashMap<String, Object>> rules;

        private Builder() {
            tableType(TABLE_TYPE);
        }

        public Builder rules(List<LinkedHashMap<String, Object>> rules) {
            this.rules = rules;
            return this;
        }

        public Builder headers(List<SimpleRuleHeaderView> headers) {
            this.headers = headers;
            return this;
        }

        @Override
        protected Builder self() {
            return this;
        }

        public SimpleRulesView build() {
            return new SimpleRulesView(this);
        }
    }

}
