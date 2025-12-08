package org.openl.studio.projects.model.tables;

import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;

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

    @Schema(description = "Kind of the table object",
            allowableValues = {
                    "Rules",
                    "Spreadsheet",
                    "Datatype",
                    "Data",
                    "Test",
                    "TBasic",
                    "Column Match",
                    "Method",
                    "Run",
                    "Constants",
                    "Conditions",
                    "Actions",
                    "Returns",
                    "Environment",
                    "Properties",
                    "Other"
            })
    public final String kind;

    @Schema(description = "Name of the table")
    public final String name;

    @Schema(description = "Custom properties associated with the table")
    public final Map<String, Object> properties;

    protected TableView(Builder<?> builder) {
        this.id = builder.id;
        this.tableType = builder.tableType;
        this.name = builder.name;
        this.kind = builder.kind;
        this.properties = Optional.ofNullable(builder.properties).map(Map::copyOf).orElse(Map.of());
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
