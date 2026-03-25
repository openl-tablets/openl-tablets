package org.openl.rules.webstudio.web.repository;

import java.util.Collection;

import lombok.Builder;

import org.openl.rules.common.CommonUser;
import org.openl.rules.common.ProjectDescriptor;

@Builder
public record DeploymentRequest(
        String productionRepositoryId,
        String name,
        Collection<ProjectDescriptor> projectDescriptors,
        CommonUser currentUser,
        String comment
) {
}
