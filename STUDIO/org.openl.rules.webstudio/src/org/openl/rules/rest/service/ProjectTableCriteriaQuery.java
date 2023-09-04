/* Copyright © 2023 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package org.openl.rules.rest.service;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Project criteria query. Used to filter project tables in {@link ProjectService}.
 *
 * @author Vladyslav Pikus
 */
public class ProjectTableCriteriaQuery {

    private final Collection<String> kinds;
    private final String name;
    private final Map<String, Object> properties;

    private ProjectTableCriteriaQuery(Builder builder) {
        this.kinds = builder.kinds == null ? Collections.emptyList()
                                           : Collections.unmodifiableCollection(builder.kinds);
        this.name = builder.name;
        this.properties = builder.properties == null ? Collections.emptyMap() : Map.copyOf(builder.properties);
    }

    public Collection<String> getKinds() {
        return kinds;
    }

    public Optional<String> getName() {
        return Optional.ofNullable(name);
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Collection<String> kinds;
        private String name;
        private Map<String, Object> properties = new HashMap<>();

        public Builder kinds(Collection<String> kinds) {
            this.kinds = kinds;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder property(String name, Object value) {
            this.properties.put(name, value);
            return this;
        }

        public ProjectTableCriteriaQuery build() {
            return new ProjectTableCriteriaQuery(this);
        }
    }

}
