package org.openl.studio.projects.service.tables.graph;

/**
 * Direction in which the table dependency graph is traversed.
 *
 * @author Vladyslav Pikus
 */
public enum GraphDirection {

    /**
     * Downstream relations: the tables a table depends on.
     */
    DEPENDENCIES,

    /**
     * Upstream relations: the tables that depend on a table.
     */
    DEPENDENTS,

    /**
     * Both downstream and upstream relations.
     */
    BOTH;

    public boolean includesDependencies() {
        return this != DEPENDENTS;
    }

    public boolean includesDependents() {
        return this != DEPENDENCIES;
    }
}
