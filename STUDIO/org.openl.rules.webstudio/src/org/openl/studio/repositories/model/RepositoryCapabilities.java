package org.openl.studio.repositories.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.Builder;

/**
 * Capabilities the current user has on a repository.
 *
 * <p>Each flag is computed server-side, honouring both the ACL and the repository configuration (which
 * can take precedence over the ACL). The UI only shows or hides controls; the server enforces every
 * operation independently.
 *
 * <p>A capability that is not granted is {@code null} (omitted) rather than {@code false}, to keep the
 * response small.
 */
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RepositoryCapabilities(
        @Parameter(description = "Whether the user can create a new project in this design repository") Boolean canCreateProject,
        @Parameter(description = "Whether the user can manage the repository access rights") Boolean canManage) {
}
