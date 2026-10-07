package org.openl.studio.projects.model.trace;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import org.jspecify.annotations.Nullable;

/**
 * A debug session failure shaped for non-technical users.
 *
 * <p>{@code summary} is the cleaned, business-readable message. {@code table} and {@code location} say
 * where it happened. {@code type} and {@code detail} carry the technical exception type and stack trace
 * for an optional drill-down, so the everyday view stays free of Java jargon.
 *
 * @param summary  cleaned failure message
 * @param table    name of the table that failed, or {@code null}
 * @param location current line inside that table (cell or rule), or {@code null}
 * @param type     exception type name, for the technical drill-down, or {@code null}
 * @param detail   stack trace, for the technical drill-down, or {@code null}
 */
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "trace.type.debug-error.desc")
public record DebugError(
        @Schema(description = "trace.field.error.summary.desc")
        String summary,

        @Schema(description = "trace.field.error.table.desc")
        @Nullable String table,

        @Schema(description = "trace.field.error.location.desc")
        @Nullable String location,

        @Schema(description = "trace.field.error.type.desc")
        @Nullable String type,

        @Schema(description = "trace.field.error.detail.desc")
        @Nullable String detail
) {
}
