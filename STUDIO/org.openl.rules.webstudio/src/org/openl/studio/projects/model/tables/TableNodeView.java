package org.openl.studio.projects.model.tables;

import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * A node of the project tables dependency graph.
 *
 * <p>Carries what every node has: the identity of the table it stands for, where it lives, the project that owns it,
 * and its relations to the other nodes. What a node adds on top depends on what it stands for — an
 * {@link ExecutableNodeView} describes a callable table, a {@link DatatypeNodeView} describes a datatype and the data
 * model around it.
 *
 * <p>Its {@link #kind} is a {@link TableGraphNodeKind} rather than a {@link TableKind}, because a node can also be the
 * synthetic dispatcher of same-named versions, which is not a table. The kind is also what tells the two node shapes
 * apart, so every kind is mapped to one of them below — a new kind has to be registered here too. A node does not
 * extend {@link SummaryTableView}:
 * the fields are declared here so the schema can carry the graph node kind, which the shared summary kind cannot.
 * Summary fields that are not relevant to the graph are left unset and omitted from the response.
 *
 * @author Vladyslav Pikus
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "kind", visible = true)
@JsonSubTypes({
        @JsonSubTypes.Type(value = DatatypeNodeView.class, name = "Datatype"),
        @JsonSubTypes.Type(value = ExecutableNodeView.class, names = {"Rules", "Spreadsheet", "Data", "Test",
                "TBasic", "Column Match", "Method", "Run", "Constants", "Conditions", "Actions", "Returns",
                "Environment", "Properties", "Other", "Dispatcher"})
})
@Schema(description = "A node of the tables dependency graph")
public abstract class TableNodeView {

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

    protected TableNodeView(Builder<?> builder) {
        this.id = builder.id;
        this.tableType = builder.tableType;
        this.kind = builder.kind;
        this.name = builder.name;
        this.properties = builder.properties;
        this.file = builder.file;
        this.pos = builder.pos;
        this.project = builder.project;
        this.dependencies = builder.dependencies;
        this.dependents = builder.dependents;
    }

    public abstract static class Builder<T extends Builder<T>> {
        private String id;
        private String tableType;
        private TableGraphNodeKind kind;
        private String name;
        private Map<String, Object> properties;
        private String file;
        private String pos;
        private String project;
        private Set<String> dependencies;
        private Set<String> dependents;

        protected abstract T self();

        /**
         * Copies the summary fields every node shares, except the kind — the graph node's kind is set separately,
         * because it can be the dispatcher rather than a table kind. Graph-specific fields (project, dependencies,
         * dependents) and any id/name override are applied afterwards.
         */
        public T summary(SummaryTableView source) {
            this.id = source.id;
            this.name = source.name;
            this.tableType = source.tableType;
            this.properties = source.properties;
            this.file = source.file;
            this.pos = source.pos;
            return self();
        }

        public T id(String id) {
            this.id = id;
            return self();
        }

        public T name(String name) {
            this.name = name;
            return self();
        }

        public T kind(TableGraphNodeKind kind) {
            this.kind = kind;
            return self();
        }

        public T tableType(String tableType) {
            this.tableType = tableType;
            return self();
        }

        public T project(String project) {
            this.project = project;
            return self();
        }

        public T dependencies(Set<String> dependencies) {
            this.dependencies = dependencies;
            return self();
        }

        public T dependents(Set<String> dependents) {
            this.dependents = dependents;
            return self();
        }

        public abstract TableNodeView build();
    }
}
