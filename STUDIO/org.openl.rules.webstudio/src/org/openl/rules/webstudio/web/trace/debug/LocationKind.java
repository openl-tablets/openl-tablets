package org.openl.rules.webstudio.web.trace.debug;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * The kind of line a stack frame can stand on.
 *
 * <p>Each constant serializes to a stable code instead of the enum name, so the wire value stays stable
 * and human-readable.
 */
@RequiredArgsConstructor
public enum LocationKind {

    /** A spreadsheet cell. */
    @JsonProperty("cell")
    CELL("cell"),

    /** A decision-table fired rule. */
    @JsonProperty("dtrule")
    DT_RULE("dtrule"),

    /** A TBasic algorithm operation. */
    @JsonProperty("operation")
    OPERATION("operation");

    /** The stable wire code, also the fallback step key for a location with no reference and no label. */
    @Getter
    private final String code;
}
