package org.openl.studio.projects.model.merge;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * A conflicted workbook as the merge reads it: the sheets either branch changed, how the merge takes each of them by
 * itself, and what both branches changed in ways the merge cannot combine.
 *
 * <p>A conflict is resolved by taking what conflicts, a table or a whole sheet, from one version. Everything else is
 * merged as Sync merges it.
 *
 * @param sheets  the sheets either branch changed, in the order of your version and then of their version
 * @param palette whether both branches changed the colours of the palette of an .xls workbook differently
 */
@Schema(description = "A conflicted workbook as the merge reads it: the sheets either branch changed, and the conflicts")
public record ConflictWorkbookView(
        @Parameter(description = "The sheets either branch changed, in the order of your version and then of their "
                + "version")
        @JsonInclude(JsonInclude.Include.ALWAYS)
        List<ConflictSheetView> sheets,

        @Parameter(description = "Whether both branches changed the colours of the palette of an .xls workbook "
                + "differently. The palette is resolved as a whole, by your or their version, under the identifier "
                + "`/palette`")
        boolean palette
) {
}
