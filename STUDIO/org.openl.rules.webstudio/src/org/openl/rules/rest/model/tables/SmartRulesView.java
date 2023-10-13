/* Copyright © 2023 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package org.openl.rules.rest.model.tables;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

/**
 * TODO description
 *
 * @author Vladyslav Pikus
 */
@JsonDeserialize(builder = SmartRulesView.Builder.class)
public class SmartRulesView extends ExecutableView {

    public static final String TABLE_TYPE = "SmartRules";

    public final List<SmartRulesHeaderView> headers;

    public final List<LinkedHashMap<String, Object>> rules;

    public SmartRulesView(Builder builder) {
        super(builder);
        this.headers = Optional.ofNullable(builder.headers).map(List::copyOf).orElse(List.of());
        this.rules = Optional.ofNullable(builder.rules).map(List::copyOf).orElse(List.of());
    }

    @JsonCreator
    public static Builder builder() {
        return new Builder();
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder extends ExecutableView.Builder<Builder> {

        private List<SmartRulesHeaderView> headers;
        private List<LinkedHashMap<String, Object>> rules;

        public Builder() {
        }

        public Builder headers(List<SmartRulesHeaderView> headers) {
            this.headers = headers;
            return this;
        }

        public Builder rules(List<LinkedHashMap<String, Object>> rules) {
            this.rules = rules;
            return this;
        }

        @Override
        protected Builder self() {
            return this;
        }

        @Override
        public SmartRulesView build() {
            return new SmartRulesView(this);
        }
    }
}
