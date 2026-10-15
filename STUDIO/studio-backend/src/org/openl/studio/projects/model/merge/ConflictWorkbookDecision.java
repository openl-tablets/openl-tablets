package org.openl.studio.projects.model.merge;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * How one conflict of a workbook is resolved: what conflicts is taken whole from one version.
 *
 * @param id   the conflict: the identifier of a table, the name of a sheet, or {@code /palette}
 * @param side the version it is taken from
 */
@Schema(description = "How one conflict of a workbook is resolved: what conflicts is taken whole from one version")
public record ConflictWorkbookDecision(
        @Parameter(description = "The conflict: the identifier of a table conflict, the name of a sheet, which takes "
                + "the whole sheet, or `/palette`", required = true)
        @NotBlank
        String id,

        @Parameter(description = "The version it is taken from. The palette is taken from your or their version only",
                required = true)
        @NotNull
        ConflictBase side
) {
}
