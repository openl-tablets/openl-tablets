package org.openl.rules.rest.acl.service;

import org.springframework.security.acls.model.Permission;

import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.AProjectArtefact;

public interface AclProjectsHelper {

    boolean hasPermission(AProject project, Permission permission);

    boolean hasPermission(AProjectArtefact child, Permission permission);

    boolean hasCreateProjectPermission(String repoId);

    boolean hasCreateDeployConfigProjectPermission();

}
