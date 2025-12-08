package org.openl.studio.projects.model.tables;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import io.swagger.v3.oas.annotations.media.Schema;

import org.openl.util.CollectionUtils;

/**
 * {@code SimpleRules} table model
 *
 * @author Vladyslav Pikus
 */
@JsonDeserialize(builder = SimpleRulesView.Builder.class)
public class SimpleRulesView extends ExecutableView {

    private static final int DEFAULT_HEADER_HEIGHT = 1;

    public static final String TABLE_TYPE = "SimpleRules";

    @Schema(description = "List of rule headers")
    public final List<SimpleRuleHeaderView> headers;

    @Schema(description = "List of rule rows as key-value maps")
    public final List<LinkedHashMap<String, Object>> rules;

    public SimpleRulesView(Builder builder) {
        super(builder);
        this.rules = Optional.ofNullable(builder.rules).map(List::copyOf).orElseGet(List::of);
        this.headers = Optional.ofNullable(builder.headers).map(List::copyOf).orElseGet(List::of);
    }

    @Override
    protected int getBodyHeight() {
        var height = CollectionUtils.isNotEmpty(rules) ? rules.size() : 0;
        return height + DEFAULT_HEADER_HEIGHT;
    }

    @Override
    protected int getBodyWidth() {
        return CollectionUtils.isNotEmpty(headers) ? headers.size() : 0;
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
