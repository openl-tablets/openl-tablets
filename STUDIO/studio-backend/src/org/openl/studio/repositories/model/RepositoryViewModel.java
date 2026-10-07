package org.openl.studio.repositories.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.Builder;

import org.openl.rules.rest.acl.model.AclRepositoryId;

@Builder
public record RepositoryViewModel(
        @Parameter(description = "Repository unique identifier for ACL", required = true)
        AclRepositoryId aclId,
        @Parameter(description = "Repository unique identifier. Used as identifier in all requests", required = true)
        String id,
        @Parameter(description = "Repository display name", required = true)
        String name,
        @Parameter(description = "Repository implementation type")
        String type,
        @Parameter(description = "Capabilities of the current user on the repository")
        RepositoryCapabilities capabilities,
        @Parameter(description = "Repository storage features")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        RepositoryFeatures features,
        @Parameter(description = "Whether the repository takes a project only from the main branch of its design repository")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        Boolean mainBranchOnly) {
}
