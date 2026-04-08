package org.openl.studio.projects.model.modules;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Response body for the copy module operation.
 */
@Schema(description = "Result of copying a module")
public record CopyModuleResponse(

        @Schema(description = "Name of the newly created module")
        String moduleName,

        @Schema(description = "File path of the new module relative to project root")
        String path,

        @Schema(description = "True if the module is auto-discovered by a wildcard pattern and not explicitly added to the descriptor")
        boolean wildcardCovered
) {
}
