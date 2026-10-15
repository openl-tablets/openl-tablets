package org.openl.studio.projects.model.merge;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

/**
 * A sheet of a conflicted workbook either branch changed.
 *
 * <p>A conflicting sheet is resolved table by table, or only as a whole when its conflict lies outside its tables.
 * Either way the sheet can be taken whole from one version, which resolves its tables too. The name of the sheet
 * identifies it in a resolution.
 *
 * @param name   the name of the sheet
 * @param status how the merge takes the sheet by itself
 * @param reason why the sheet is resolved only as a whole, or {@code null} when it is not
 * @param ours   whether your version has the sheet
 * @param base   whether the base version has the sheet
 * @param theirs whether their version has the sheet
 * @param tables the conflicting tables of the sheet, from top to bottom
 */
@Schema(description = "A sheet of a conflicted workbook either branch changed")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ConflictSheetView(
        @Parameter(description = "The name of the sheet, which identifies the sheet in a resolution")
        String name,

        @Parameter(description = "How the merge takes the sheet by itself")
        ConflictSheetStatus status,

        @Parameter(description = "Why the sheet is resolved only as a whole; absent when its tables are resolved one "
                + "by one, or the sheet is no conflict")
        @Nullable ConflictSheetReason reason,

        @Parameter(description = "Whether your version has the sheet")
        boolean ours,

        @Parameter(description = "Whether the base version has the sheet")
        boolean base,

        @Parameter(description = "Whether their version has the sheet")
        boolean theirs,

        @Parameter(description = "The conflicting tables of the sheet, from top to bottom; empty unless the sheet is "
                + "resolved table by table")
        @JsonInclude(JsonInclude.Include.ALWAYS)
        List<ConflictTableView> tables
) {
}
