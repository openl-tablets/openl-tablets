package org.openl.studio.projects.model.tables;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Kind of a node in the tables dependency graph.
 * <p>
 * A graph node is either a table — carrying any {@link TableKind} — or the synthetic {@code Dispatcher} node the graph
 * shows for a runtime dispatcher of same-named versions. The table-kind wire values are reused from {@link TableKind},
 * so the labels stay defined in one place.
 *
 * @author Vladyslav Pikus
 */
@Schema(description = "Kind of a graph node: a table kind, or the runtime dispatcher of same-named versions")
public enum TableGraphNodeKind {

    RULES(TableKind.RULES),
    SPREADSHEET(TableKind.SPREADSHEET),
    DATATYPE(TableKind.DATATYPE),
    DATA(TableKind.DATA),
    TEST(TableKind.TEST),
    TBASIC(TableKind.TBASIC),
    COLUMN_MATCH(TableKind.COLUMN_MATCH),
    METHOD(TableKind.METHOD),
    RUN(TableKind.RUN),
    CONSTANTS(TableKind.CONSTANTS),
    CONDITIONS(TableKind.CONDITIONS),
    ACTIONS(TableKind.ACTIONS),
    RETURNS(TableKind.RETURNS),
    ENVIRONMENT(TableKind.ENVIRONMENT),
    PROPERTIES(TableKind.PROPERTIES),
    OTHER(TableKind.OTHER),

    /** Not a table kind: the runtime dispatcher of same-named versions. */
    DISPATCHER("Dispatcher");

    private final String value;

    TableGraphNodeKind(TableKind kind) {
        this.value = kind.value();
    }

    TableGraphNodeKind(String value) {
        this.value = value;
    }

    /** The wire value: a table kind's label, or {@code Dispatcher}. */
    @JsonValue
    public String value() {
        return value;
    }

    private static final Map<String, TableGraphNodeKind> BY_VALUE = byValue();

    private static Map<String, TableGraphNodeKind> byValue() {
        var map = new HashMap<String, TableGraphNodeKind>();
        for (var kind : values()) {
            map.put(kind.value, kind);
        }
        return Map.copyOf(map);
    }

    /**
     * The graph node kind for a display value, as {@code OpenLTableUtils} and the graph report it.
     *
     * @param value the display value
     * @return the matching kind
     * @throws IllegalArgumentException when no kind carries the value
     */
    public static TableGraphNodeKind fromValue(String value) {
        var kind = BY_VALUE.get(value);
        if (kind == null) {
            throw new IllegalArgumentException("Unknown graph node kind: " + value);
        }
        return kind;
    }
}
