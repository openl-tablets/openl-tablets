package org.openl.studio.projects.model;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

/**
 * One level of a value: a page of the lines it opens into, or the value itself when it opens into none.
 *
 * @param type     the display name of the type of the value
 * @param total    how many lines the value opens into, on every page
 * @param elements whether the lines are elements rather than fields
 * @param lines    the lines of the page
 * @param value    the value as it is written, when it opens into no lines
 */
@Schema(description = "value.level.desc")
public record ValueLevel(
        @Schema(description = "value.level.field.type.desc")
        @Nullable String type,

        @Schema(description = "value.level.field.total.desc")
        int total,

        @Schema(description = "value.level.field.elements.desc")
        @Nullable Boolean elements,

        @Schema(description = "value.level.field.lines.desc")
        List<ValueLine> lines,

        @Schema(description = "value.level.field.value.desc", implementation = Object.class)
        @Nullable JsonNode value
) {
}
