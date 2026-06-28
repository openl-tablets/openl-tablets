package org.openl.studio.projects.model.trace;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import org.jspecify.annotations.Nullable;

/**
 * The current line being evaluated inside a stack frame.
 *
 * @param kind   short location type ({@code cell}, {@code dtrule}, {@code operation})
 * @param row    cell row index, or {@code null}
 * @param column cell column index, or {@code null}
 * @param ref    short cell reference such as {@code R2C3}, or {@code null}
 * @param label  human-readable description, or {@code null}
 */
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "trace.type.debug-location.desc")
public record DebugLocationView(
        @Schema(description = "trace.field.location.kind.desc")
        String kind,

        @Schema(description = "trace.field.location.row.desc")
        @Nullable Integer row,

        @Schema(description = "trace.field.location.column.desc")
        @Nullable Integer column,

        @Schema(description = "trace.field.location.ref.desc")
        @Nullable String ref,

        @Schema(description = "trace.field.location.label.desc")
        @Nullable String label
) {
}
