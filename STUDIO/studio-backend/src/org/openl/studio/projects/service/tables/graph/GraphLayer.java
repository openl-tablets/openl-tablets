package org.openl.studio.projects.service.tables.graph;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Which layer of the project graph to build.
 *
 * <p>The two layers are disjoint: a callable table is linked to the tables it calls, a datatype to the datatypes it is
 * built from, and nothing links one layer to the other. Asking for a single layer therefore returns the same nodes and
 * the same relations it would have in the whole graph, only without the other layer's tables.
 *
 * @author Vladyslav Pikus
 */
public enum GraphLayer {

    /**
     * Callable tables only: rules, spreadsheets, methods and the dispatchers of same-named versions.
     */
    @JsonProperty("executable")
    EXECUTABLE,

    /**
     * Datatype tables only: the data model.
     */
    @JsonProperty("datatype")
    DATATYPE,

    /**
     * Both layers.
     */
    @JsonProperty("all")
    ALL;

    public boolean includesExecutable() {
        return this != DATATYPE;
    }

    public boolean includesDatatypes() {
        return this != EXECUTABLE;
    }
}
