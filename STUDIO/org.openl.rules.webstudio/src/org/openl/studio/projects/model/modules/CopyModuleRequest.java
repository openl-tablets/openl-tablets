package org.openl.studio.projects.model.modules;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Request body for the copy module operation.
 */
@Schema(description = "Request to copy a module within a project")
public record CopyModuleRequest(

        @Schema(description = "New module name. Required if the target path is NOT covered by a wildcard pattern. "
                + "If omitted and the target IS wildcard-covered, the name is derived from the file name.")
        String newModuleName,

        @Schema(description = "Target file path for the copy, relative to project root. "
                + "If omitted, calculated from the source path directory and new module name "
                + "(e.g., source 'rules/Src.xlsx' + newModuleName 'Copy' -> 'rules/Copy.xlsx').")
        String newPath
) {
}
