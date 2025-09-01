package org.openl.rules.rest.acl.service;

import java.util.Collection;

import org.springframework.security.acls.model.Permission;

import org.openl.rules.common.ProjectDescriptor;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.AProjectArtefact;
import org.openl.rules.webstudio.web.repository.DeploymentRequest;

public interface AclProjectsHelper {

    boolean hasPermission(AProject project, Permission permission);

    boolean hasPermission(AProjectArtefact child, Permission permission);

    boolean hasCreateProjectPermission(String repoId);

    @Deprecated
    boolean hasCreateDeployConfigProjectPermission();

    boolean hasCreateDeploymentPermission(String repoId);

    boolean hasPermission(Collection<ProjectDescriptor> projects, Permission permission);

    boolean hasPermission(DeploymentRequest deploymentRequest, Permission permission);

}
