package org.openl.studio.projects.model.tables;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

import org.openl.binding.impl.NodeType;

/**
 * What a piece of a cell's text stands for, as the compiler read it.
 *
 * <p>The Editor draws each kind differently — a rule is not a field, and a field is not a type — so the kind
 * crosses with the usage rather than being guessed from the text.
 */
@Schema(description = "What a piece of a cell's text refers to")
public enum RawTableUsageKind {

    @Schema(description = "Another rules table")
    @JsonProperty("rule")
    RULE,

    @Schema(description = "A datatype")
    @JsonProperty("datatype")
    DATATYPE,

    @Schema(description = "A data table")
    @JsonProperty("data")
    DATA,

    @Schema(description = "A field of a datatype")
    @JsonProperty("field")
    FIELD,

    @Schema(description = "Something else the compiler underlines")
    @JsonProperty("underlined")
    OTHER_UNDERLINED,

    @Schema(description = "Something else the compiler knows")
    @JsonProperty("other")
    OTHER;

    /**
     * The kind as this API names it.
     *
     * @param nodeType kind the compiler reported, or {@code null} when it reported none
     * @return the kind, or {@link #OTHER} for one this API does not name separately
     */
    public static RawTableUsageKind of(@Nullable NodeType nodeType) {
        if (nodeType == null) {
            return OTHER;
        }
        return switch (nodeType) {
            case RULE -> RULE;
            case DATATYPE -> DATATYPE;
            case DATA -> DATA;
            case FIELD -> FIELD;
            case OTHERUNDERLINED -> OTHER_UNDERLINED;
            default -> OTHER;
        };
    }
}
