package org.openl.studio.projects.model.tables;

import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * A node of the project tables dependency graph.
 *
 * <p>Carries the summary fields of the table it stands for plus the owning project name and the table's relations to
 * other tables. Its {@link #kind} is a {@link TableGraphNodeKind} rather than a {@link TableKind}, because a node can
 * also be the synthetic dispatcher of same-named versions, which is not a table. It does not extend
 * {@link SummaryTableView}: the fields are declared here so the schema can carry the graph node kind, which the shared
 * summary kind cannot. Summary fields that are not relevant to the graph are left unset and omitted from the response.
 *
 * @author Vladyslav Pikus
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class TableNodeView {

    @Schema(description = "Unique identifier of the table")
    public final String id;

    @Schema(description = "Type of the table (e.g., 'Datatype', 'Vocabulary', 'Spreadsheet', etc.)")
    public final String tableType;

    @Schema(description = "Kind of the graph node")
    public final TableGraphNodeKind kind;

    @Schema(description = "Name of the table")
    public final String name;

    @Schema(description = "Custom properties associated with the table")
    public final Map<String, Object> properties;

    @Schema(description = "Return type of the table (e.g., Integer, String, etc.)")
    public final String returnType;

    @Schema(description = "Signature of the table")
    public final String signature;

    @Schema(description = "File where the table is located")
    public final String file;

    @Schema(description = "Position of the table within the file")
    public final String pos;

    @Schema(description = "Name of the project that owns the table")
    public final String project;

    @Schema(description = "Identifiers of the tables this table depends on")
    public final Set<String> dependencies;

    @Schema(description = "Identifiers of the tables that depend on this table")
    public final Set<String> dependents;

    @Schema(description = """
            Dimension properties this version of the table is selected by — the versioning rules the \
            dispatcher uses (e.g. state, lob, dates), resolved from the module name pattern or the table itself""")
    public final Map<String, String> dimensionProperties;

    private TableNodeView(Builder builder) {
        this.id = builder.id;
        this.tableType = builder.tableType;
        this.kind = builder.kind;
        this.name = builder.name;
        this.properties = builder.properties;
        this.returnType = builder.returnType;
        this.signature = builder.signature;
        this.file = builder.file;
        this.pos = builder.pos;
        this.project = builder.project;
        this.dependencies = builder.dependencies;
        this.dependents = builder.dependents;
        this.dimensionProperties = builder.dimensionProperties;
    }

    public static final class Builder {
        private String id;
        private String tableType;
        private TableGraphNodeKind kind;
        private String name;
        private Map<String, Object> properties;
        private String returnType;
        private String signature;
        private String file;
        private String pos;
        private String project;
        private Set<String> dependencies;
        private Set<String> dependents;
        private Map<String, String> dimensionProperties;

        /**
         * Copies every {@link SummaryTableView} field except the kind — the graph node's kind is set separately,
         * because it can be the dispatcher rather than a table kind. Graph-specific fields (project, dependencies,
         * dependents) and any id/name override are applied afterwards.
         */
        public Builder summary(SummaryTableView source) {
            this.id = source.id;
            this.name = source.name;
            this.tableType = source.tableType;
            this.properties = source.properties;
            this.returnType = source.returnType;
            this.signature = source.signature;
            this.file = source.file;
            this.pos = source.pos;
            return this;
        }

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder kind(TableGraphNodeKind kind) {
            this.kind = kind;
            return this;
        }

        public Builder tableType(String tableType) {
            this.tableType = tableType;
            return this;
        }

        public Builder project(String project) {
            this.project = project;
            return this;
        }

        public Builder dependencies(Set<String> dependencies) {
            this.dependencies = dependencies;
            return this;
        }

        public Builder dependents(Set<String> dependents) {
            this.dependents = dependents;
            return this;
        }

        public Builder dimensionProperties(Map<String, String> dimensionProperties) {
            this.dimensionProperties = dimensionProperties;
            return this;
        }

        public TableNodeView build() {
            return new TableNodeView(this);
        }
    }
}
