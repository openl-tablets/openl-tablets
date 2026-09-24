package org.openl.studio.projects.model;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import org.jspecify.annotations.Nullable;

/**
 * One line of a level of a value: a field, an element or an entry.
 *
 * <p>A plain value is written as it is. A value with inner structure is referred to by its type and by how many
 * lines it opens into; it is read a level at a time, by the path that ends with the segment of its line.
 *
 * @param name     what the line is shown as: the name of a field, the index of an element in brackets, or the key
 *                 of an entry
 * @param segment  what opens the value of the line: the name of a field, or the position of an element or an entry
 * @param value    the value of the line, when it is plain
 * @param type     the display name of the type of a value with inner structure
 * @param size     how many lines a value with inner structure opens into
 * @param elements whether the lines of a value with inner structure are elements rather than fields
 */
@Builder
@Schema(description = "value.line.desc")
public record ValueLine(
        @Schema(description = "value.line.field.name.desc")
        String name,

        @Schema(description = "value.line.field.segment.desc")
        String segment,

        @Schema(description = "value.line.field.value.desc", implementation = Object.class)
        @Nullable JsonNode value,

        @Schema(description = "value.line.field.type.desc")
        @Nullable String type,

        @Schema(description = "value.line.field.size.desc")
        @Nullable Integer size,

        @Schema(description = "value.line.field.elements.desc")
        @Nullable Boolean elements
) {
}
