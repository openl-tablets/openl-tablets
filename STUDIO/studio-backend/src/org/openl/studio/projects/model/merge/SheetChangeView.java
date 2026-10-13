package org.openl.studio.projects.model.merge;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * A sheet of a workbook the merge combined from both branches by itself.
 */
@Schema(description = "A sheet of a workbook merged automatically")
public record SheetChangeView(
        @Parameter(description = "Name of the sheet")
        String name,

        @Parameter(description = "Which branch the sheet was taken from")
        SheetOrigin takenFrom
) {
}
