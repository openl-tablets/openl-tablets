package org.openl.rules.rest.acl.service;

import org.springframework.security.acls.model.Permission;

import org.openl.rules.rest.acl.model.AclRepositoryId;

public interface AclRepositoriesHelper {

    boolean hasPermission(AclRepositoryId repositoryId, Permission permission);

}
