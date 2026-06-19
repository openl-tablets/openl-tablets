package org.openl.studio.projects.model.tables;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * A node of the project tables dependency graph.
 *
 * <p>Extends {@link SummaryTableView} with the owning project name and the table's relations to other tables. Summary
 * fields that are not relevant to the graph are left unset and omitted from the response.
 *
 * @author Vladyslav Pikus
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class TableNodeView extends SummaryTableView {

    @Schema(description = "Name of the project that owns the table")
    public final String project;

    @Schema(description = "Identifiers of the tables this table depends on")
    public final Set<String> dependencies;

    @Schema(description = "Identifiers of the tables that depend on this table")
    public final Set<String> dependents;

    private TableNodeView(Builder builder) {
        super(builder.summary);
        this.project = builder.project;
        this.dependencies = builder.dependencies;
        this.dependents = builder.dependents;
    }

    /**
     * Builder that reuses {@link SummaryTableView.Builder} for the inherited summary fields and adds the graph-specific
     * ones. Kept as a thin wrapper so that {@link SummaryTableView} stays free of any graph concerns.
     */
    public static final class Builder {
        private final SummaryTableView.Builder summary = SummaryTableView.builder();
        private String project;
        private Set<String> dependencies;
        private Set<String> dependents;

        public Builder id(String id) {
            summary.id(id);
            return this;
        }

        public Builder name(String name) {
            summary.name(name);
            return this;
        }

        public Builder kind(String kind) {
            summary.kind(kind);
            return this;
        }

        public Builder tableType(String tableType) {
            summary.tableType(tableType);
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

        public TableNodeView build() {
            return new TableNodeView(this);
        }
    }
}
