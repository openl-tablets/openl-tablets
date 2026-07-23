package org.openl.studio.projects.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.Builder;

import org.openl.studio.common.model.Capabilities;

/**
 * Capabilities the current user has on a project: the base project {@link Capabilities} plus the project
 * lifecycle. The base capabilities are flattened into this object, so the response stays flat while the
 * base set is reused.
 *
 * <p>Each flag is computed server-side from the user's effective access and the project state (opened,
 * opened-for-editing, locked, local-only, modified, branch protection). The UI only shows or hides
 * controls; the server enforces every operation independently.
 *
 * <p>A capability that is not granted is {@code null} (omitted) rather than {@code false}, to keep the
 * response small.
 */
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProjectCapabilities(
        @JsonUnwrapped Capabilities project,
        @Parameter(description = "Whether the project can be opened for editing") Boolean canOpen,
        @Parameter(description = "Whether the opened project can be closed") Boolean canClose,
        @Parameter(description = "Whether local modifications can be saved (committed)") Boolean canSave,
        @Parameter(description = "Whether the project lock can be forcibly released") Boolean canUnlock,
        @Parameter(description = "Whether the project can be deployed") Boolean canDeploy,
        @Parameter(description = "Whether project revisions can be compared") Boolean canCompare,
        @Parameter(description = "Whether the project history can be viewed") Boolean canViewHistory,
        @Parameter(description = "Whether the user can manage the project access rights") Boolean canManage,
        @Parameter(description = "Whether the project can be copied into a new project") Boolean canCopy,
        @Parameter(description = "Whether the project branches can be managed — created, merged and deleted") Boolean canManageBranches,
        @Parameter(description = "Whether the project can be exported as an archive") Boolean canExport) {
}
