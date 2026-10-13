package org.openl.studio.projects.model.merge;

import java.util.List;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;

import org.openl.studio.projects.model.project.status.ChangeType;

/**
 * A file a merge changed in the target branch.
 */
@Schema(description = "A file the merge changed in the target branch")
public record FileChangeView(
        @Parameter(description = "Path of the file from the root of the repository")
        String path,

        @Parameter(description = "What the merge did to the file")
        ChangeType type,

        @Parameter(description = "For a workbook both branches changed and the merge combined without a decision of the user, the sheets either branch changed and the branch each was taken from; absent otherwise")
        List<SheetChangeView> sheets
) {
}
