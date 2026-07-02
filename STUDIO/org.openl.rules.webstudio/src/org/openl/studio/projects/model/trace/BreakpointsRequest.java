package org.openl.studio.projects.model.trace;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

/**
 * Breakpoint set sent by the UI.
 *
 * @param uris table source URIs that should suspend execution on entry
 */
@Schema(description = "trace.type.breakpoints.desc")
public record BreakpointsRequest(
        @Schema(description = "trace.field.breakpoints.uris.desc")
        @Nullable List<String> uris
) {

    public List<String> safeUris() {
        return uris == null ? List.of() : uris;
    }
}
