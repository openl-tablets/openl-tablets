package org.openl.rules.rest.acl.service;

import java.util.List;

import org.springframework.security.acls.model.Permission;

import org.openl.rules.project.abstraction.AProject;
import org.openl.security.acl.repository.RepositoryAclServiceProvider;

public class AclProjectsHelperImpl implements AclProjectsHelper {

    private final RepositoryAclServiceProvider aclServiceProvider;

    public AclProjectsHelperImpl(RepositoryAclServiceProvider aclServiceProvider) {
        this.aclServiceProvider = aclServiceProvider;
    }

    @Override
    public boolean hasPermission(AProject project, Permission permission) {
        return aclServiceProvider.getDesignRepoAclService().isGranted(project, List.of(permission));
    }
}
