package org.openl.rules.rest.model.tables;

import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * TODO description
 *
 * @author Vladyslav Pikus
 */
public abstract class TableView {

    public final String id;
    public final String tableType;
    public final String kind;
    public final String name;
    public final Map<String, Object> properties;

    protected TableView(Builder<?> builder) {
        this.id = builder.id;
        this.tableType = builder.tableType;
        this.name = builder.name;
        this.kind = builder.kind;
        this.properties = Optional.ofNullable(builder.properties).map(Map::copyOf).orElse(Map.of());
    }

    public static abstract class Builder<T extends Builder<T>> {
        private String id;
        private String tableType;
        private String kind;
        private String name;
        private Map<String, Object> properties;

        protected Builder() {
        }

        protected abstract T self();

        public T id(String id) {
            this.id = id;
            return self();
        }

        public T tableType(String tableType) {
            this.tableType = tableType;
            return self();
        }

        public T kind(String kind) {
            this.kind = kind;
            return self();
        }

        public T name(String name) {
            this.name = name;
            return self();
        }

        public T properties(Map<String, Object> properties) {
            this.properties = properties;
            return self();
        }

        public abstract TableView build();
    }
}
