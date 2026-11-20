package org.openl.rules.webstudio.web.repository;

import java.nio.file.Path;
import java.util.Collection;

import lombok.Builder;
import org.jspecify.annotations.Nullable;

import org.openl.rules.common.CommonUser;
import org.openl.rules.common.ProjectDescriptor;

/**
 * @param extras           what the deployment carries from the Maven repository; nothing when it is left out
 * @param downloadedFolder the folder of the deployed project when it is downloaded from the Maven repository: its
 *                         {@code rules-deploy.xml} tells the API version of the deployment
 */
@Builder
public record DeploymentRequest(
        String productionRepositoryId,
        String name,
        Collection<ProjectDescriptor> projectDescriptors,
        CommonUser currentUser,
        String comment,
        @Nullable DeploymentExtras extras,
        @Nullable Path downloadedFolder
) {

    public DeploymentRequest {
        extras = extras == null ? DeploymentExtras.NONE : extras;
    }
}
