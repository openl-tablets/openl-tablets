package org.openl.rules.rest.acl.service;

import java.util.List;

import org.springframework.security.acls.model.Permission;

import org.openl.rules.rest.acl.model.AclRepositoryId;
import org.openl.security.acl.repository.RepositoryAclServiceProvider;

public class AclRepositoriesHelperImpl implements AclRepositoriesHelper {

    private final RepositoryAclServiceProvider aclServiceProvider;

    public AclRepositoriesHelperImpl(RepositoryAclServiceProvider aclServiceProvider) {
        this.aclServiceProvider = aclServiceProvider;
    }

    @Override
    public boolean hasPermission(AclRepositoryId repositoryId, Permission permission) {
        var aclService = aclServiceProvider.getAclService(repositoryId.getType().getType());
        return aclService.isGranted(repositoryId.getId(), null, List.of(permission));
    }
}
