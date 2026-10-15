package org.openl.studio.projects.rest.model;

import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;

import org.openl.studio.projects.model.merge.ConflictWorkbookDecision;

/**
 * How the conflicts of a workbook are resolved.
 *
 * @param decisions how each conflict is resolved
 */
@Schema(description = "How the conflicts of a workbook are resolved")
public record ResolveConflictWorkbookRequest(
        @Parameter(description = "How each conflict is resolved: every conflict of the workbook needs a decision, and "
                + "a sheet decided as a whole needs none for its tables", required = true)
        @NotNull
        List<@Valid @NotNull ConflictWorkbookDecision> decisions
) {
}
