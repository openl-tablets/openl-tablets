package org.openl.studio.projects.model.merge;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

/**
 * How one conflict of a workbook is resolved: what conflicts is taken whole from one version, or a table is merged
 * cell by cell. A decision gives one of the two.
 *
 * @param id    the conflict: the identifier of a table, the name of a sheet, or {@code /palette}
 * @param side  the version it is taken from whole, or {@code null} for a table merged cell by cell
 * @param cells how the table is merged cell by cell, or {@code null} for a conflict taken whole
 */
@Schema(description = """
        How one conflict of a workbook is resolved: what conflicts is taken whole from one version, or \
        a table is merged cell by cell""")
public record ConflictWorkbookDecision(
        @Parameter(description = """
                The conflict: the identifier of a table conflict, the name of a sheet, which takes \
                the whole sheet, or `/palette`""", required = true)
        @NotBlank
        String id,

        @Parameter(description = """
                The version it is taken from whole. The palette is taken from your or their version \
                only. Absent for a table merged cell by cell""")
        @Nullable ConflictBase side,

        @Parameter(description = """
                How the table is merged cell by cell, for a table whose conflict tells how it merges \
                so. Absent for a conflict taken whole""")
        @Valid
        @Nullable ConflictCellsDecision cells
) {
}
