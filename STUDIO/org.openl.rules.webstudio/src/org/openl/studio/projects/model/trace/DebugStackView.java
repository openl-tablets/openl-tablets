package org.openl.studio.projects.model.trace;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import org.jspecify.annotations.Nullable;

/**
 * The live execution stack at the current suspension.
 *
 * @param status       debug session status
 * @param frames       stack frames ordered from the root call to the current frame
 * @param errorMessage failure message when the session ended in error, otherwise {@code null}
 */
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "trace.type.debug-stack.desc")
public record DebugStackView(
        @Schema(description = "trace.field.stack.status.desc")
        String status,

        @Schema(description = "trace.field.stack.frames.desc")
        List<DebugFrameView> frames,

        @Schema(description = "trace.field.stack.error-message.desc")
        @Nullable String errorMessage
) {
}
