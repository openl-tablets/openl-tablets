package org.openl.studio.projects.model.tables;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;

import org.openl.studio.projects.model.project.status.DetailedMessageDescription;
import org.openl.util.CollectionUtils;

/**
 * Base class for all table views
 *
 * @author Vladyslav Pikus
 */
public abstract class TableView {

    protected static final int BASE_HEADER_HEIGHT = 1;
    protected static final int PROPERTIES_PART_WIDTH = 3;


    @Schema(description = "Unique identifier of the table")
    public final String id;

    @Schema(description = "Type of the table (e.g., 'Datatype', 'Vocabulary', 'Spreadsheet', etc.)")
    public final String tableType;

    @Schema(description = "Kind of the table object")
    public final TableKind kind;

    @Schema(description = "Name of the table")
    public final String name;

    @Schema(
            description = "Custom properties associated with the table. New tables write them to workbook rows "
                    + "in request order")
    public final Map<String, Object> properties;

    @Parameter(description = "List of messages (errors, warnings, info) related to the table")
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public List<DetailedMessageDescription> messages;

    protected TableView(Builder<?> builder) {
        this.id = builder.id;
        this.tableType = builder.tableType;
        this.name = builder.name;
        this.kind = builder.kind;
        this.properties = builder.properties == null ? Map.of() : immutableProperties(builder.properties);
    }

    private static Map<String, Object> immutableProperties(Map<String, Object> properties) {
        var copy = new LinkedHashMap<String, Object>(properties.size());
        properties.forEach((name, value) -> copy.put(Objects.requireNonNull(name), Objects.requireNonNull(value)));
        return Collections.unmodifiableMap(copy);
    }

    @JsonIgnore
    public int getHeight() {
        var propertiesHeight = CollectionUtils.isNotEmpty(properties) ? properties.size() : 0;
        return BASE_HEADER_HEIGHT + propertiesHeight + getBodyHeight();
    }

    protected abstract int getBodyHeight();

    @JsonIgnore
    public int getWidth() {
        var propertiesWidth = CollectionUtils.isNotEmpty(properties) ? PROPERTIES_PART_WIDTH : 0;
        return Math.max(getBodyWidth(), propertiesWidth);
    }

    protected abstract int getBodyWidth();

    public static abstract class Builder<T extends Builder<T>> {
        private String id;
        private String tableType;
        private TableKind kind;
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

        public T kind(TableKind kind) {
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
