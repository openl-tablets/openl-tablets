package org.openl.rules.rest.deployment.config;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.openl.rules.rest.acl.service.AclProjectsHelper;
import org.openl.rules.rest.deployment.service.DeploymentService;
import org.openl.rules.rest.deployment.service.DeploymentServiceImpl;
import org.openl.rules.webstudio.security.SecureDeploymentRepositoryService;
import org.openl.rules.webstudio.web.repository.DeploymentManager;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.studio.projects.service.ProjectDependencyResolver;
import org.openl.studio.projects.validator.ProjectStateValidator;

@Configuration
public class DeploymentConfiguration {

    @Bean
    public DeploymentService deploymentService(ProjectDependencyResolver projectDependencyResolver,
                                               SecureDeploymentRepositoryService deploymentRepositoryService,
                                               DeploymentManager deploymentManager,
                                               ObjectProvider<UserWorkspace> userWorkspaceProvider,
                                               ProjectStateValidator projectStateValidator,
                                               AclProjectsHelper aclProjectsHelper) {
        return new DeploymentServiceImpl(projectDependencyResolver,
                deploymentRepositoryService,
                deploymentManager,
                userWorkspaceProvider,
                projectStateValidator,
                aclProjectsHelper);
    }

}
