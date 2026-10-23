package org.openl.studio.projects.model.tables;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

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
            description = """
                    Custom properties associated with the table. New tables write them to workbook rows \
                    in request order""")
    public final Map<String, Object> properties;

    /**
     * The version of the table read, which the read answers in the {@code ETag} header rather than in the body: a write
     * of the table names it back in the {@code If-Match} header.
     */
    @JsonIgnore
    public String version;

    @Parameter(description = "List of messages (errors, warnings, info) related to the table")
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public List<DetailedMessageDescription> messages;

    @Parameter(description = """
            Whether the table can be run as it stands, and how far a run of it may reach. \
            A table that failed to compile, and a test whose rules failed, runs nothing""")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public TableRunState runState;

    @Parameter(description = """
            Set when the table is written as several partial tables, gathered from more than one place in the \
            workbook. Such a table is read here but not written: the cells it is drawn from do not sit \
            together, and only Excel can edit them. Absent on an ordinary table""")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public Boolean partial;

    protected TableView(Builder<?> builder) {
        this.id = builder.id;
        this.tableType = builder.tableType;
        this.name = builder.name;
        this.kind = builder.kind;
        this.properties = builder.properties == null ? Map.of() : immutableProperties(builder.properties);
    }

    private static Map<String, Object> immutableProperties(Map<String, Object> properties) {
        var copy = LinkedHashMap.<String, Object>newLinkedHashMap(properties.size());
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

    /**
     * Builds a view from a request body. A client writes a table back as it read it, so the version of the read is
     * taken in and left out: the If-Match header names the version a write is based on.
     */
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @JsonIgnoreProperties("version")
    public abstract static class Builder<T extends Builder<T>> {
        private String id;
        private String tableType;
        private TableKind kind;
        private String name;
        private Map<String, Object> properties;

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
