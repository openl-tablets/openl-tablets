package org.openl.rules.rest.acl.service;

import org.springframework.security.acls.model.Permission;

import org.openl.rules.project.abstraction.AProject;

public interface AclProjectsHelper {

    boolean hasPermission(AProject project, Permission permission);

}
