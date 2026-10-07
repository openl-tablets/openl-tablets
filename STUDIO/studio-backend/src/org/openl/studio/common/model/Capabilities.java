package org.openl.studio.common.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

/**
 * Base set of capabilities the current user has on a versioned resource (a file, a folder, or a project).
 *
 * <p>Each flag is computed server-side from the user's effective access and the resource state, so the UI
 * only shows or hides controls — it never re-derives anything from raw permissions. The server still
 * enforces every operation independently.
 *
 * <p>This base set is reused for standalone files; richer resources such as projects expose an extended
 * set that includes these fields. A capability that is not granted is {@code null} (omitted) rather than
 * {@code false}, to keep the response small.
 */
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Capabilities the current user has on a resource")
public record Capabilities(
        @Parameter(description = "Whether the user can modify the resource content") Boolean canWrite,
        @Parameter(description = "Whether the user can delete the resource") Boolean canDelete) {

    /** Serialize a granted capability as {@code true}; a denied one as {@code null} so it is omitted. */
    public static Boolean flag(boolean granted) {
        return granted ? Boolean.TRUE : null;
    }
}
