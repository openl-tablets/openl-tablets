package org.openl.studio.projects.model.trace;

import java.util.List;
import jakarta.validation.constraints.NotBlank;

import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

/**
 * Breakpoint set sent by the UI.
 *
 * <p>A key addresses a table by its source URI or by its name, optionally narrowed to one sub-step
 * ({@code #ref}) or to one execution of the table ({@code @N}), and optionally prefixed with
 * {@code after:} to suspend right after the target ran instead of before it.
 *
 * @param uris breakpoint keys that should suspend execution
 */
@Schema(description = "trace.type.breakpoints.desc")
public record BreakpointsRequest(
        @Schema(description = "trace.field.breakpoints.uris.desc")
        @Nullable List<@NotBlank String> uris
) {

    public List<String> safeUris() {
        return uris == null ? List.of() : uris;
    }
}
