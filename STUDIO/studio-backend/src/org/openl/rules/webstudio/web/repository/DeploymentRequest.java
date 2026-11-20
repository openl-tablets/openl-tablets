package org.openl.rules.webstudio.web.repository;

import java.util.Collection;

import lombok.Builder;
import org.jspecify.annotations.Nullable;

import org.openl.rules.common.CommonUser;
import org.openl.rules.common.ProjectDescriptor;

/**
 * @param extras what the deployment carries from the Maven repository; nothing when it is left out
 */
@Builder
public record DeploymentRequest(
        String productionRepositoryId,
        String name,
        Collection<ProjectDescriptor> projectDescriptors,
        CommonUser currentUser,
        String comment,
        @Nullable DeploymentExtras extras
) {

    public DeploymentRequest {
        extras = extras == null ? DeploymentExtras.NONE : extras;
    }
}
