package org.openl.studio.projects.model.tables;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Kind of an OpenL table, as the table listing and the copy dialog report it.
 * <p>
 * The wire value is the display label OpenL Studio shows (for example {@code "Column Match"}); each constant carries it
 * through {@link JsonProperty}, so the label is the single source of truth for the JSON, the OpenAPI schema and the
 * {@link #fromValue(String)} lookup.
 *
 * @author Vladyslav Pikus
 */
@Schema(description = "Kind of the table object")
public enum TableKind {

    @JsonProperty("Rules")
    RULES,
    @JsonProperty("Spreadsheet")
    SPREADSHEET,
    @JsonProperty("Datatype")
    DATATYPE,
    @JsonProperty("Data")
    DATA,
    @JsonProperty("Test")
    TEST,
    @JsonProperty("TBasic")
    TBASIC,
    @JsonProperty("Column Match")
    COLUMN_MATCH,
    @JsonProperty("Method")
    METHOD,
    @JsonProperty("Run")
    RUN,
    @JsonProperty("Constants")
    CONSTANTS,
    @JsonProperty("Conditions")
    CONDITIONS,
    @JsonProperty("Actions")
    ACTIONS,
    @JsonProperty("Returns")
    RETURNS,
    @JsonProperty("Environment")
    ENVIRONMENT,
    @JsonProperty("Properties")
    PROPERTIES,
    @JsonProperty("Other")
    OTHER;

    private static final Map<String, TableKind> BY_VALUE = byValue();

    private static Map<String, TableKind> byValue() {
        var map = new HashMap<String, TableKind>();
        for (var kind : values()) {
            map.put(kind.value(), kind);
        }
        return Map.copyOf(map);
    }

    /** The wire value declared by this constant's {@link JsonProperty}. */
    public String value() {
        try {
            return TableKind.class.getField(name()).getAnnotation(JsonProperty.class).value();
        } catch (NoSuchFieldException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * The kind for a display value, as {@code OpenLTableUtils} reports it.
     *
     * @param value the display value
     * @return the matching kind
     * @throws IllegalArgumentException when no kind carries the value
     */
    public static TableKind fromValue(String value) {
        var kind = BY_VALUE.get(value);
        if (kind == null) {
            throw new IllegalArgumentException("Unknown table kind: " + value);
        }
        return kind;
    }
}
