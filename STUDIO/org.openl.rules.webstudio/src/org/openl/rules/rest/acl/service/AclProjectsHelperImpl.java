package org.openl.rules.rest.acl.service;

import java.util.List;

import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.acls.model.Permission;

import org.openl.rules.project.abstraction.ADeploymentProject;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.UserWorkspaceProject;
import org.openl.security.acl.repository.RepositoryAclService;
import org.openl.security.acl.repository.RepositoryAclServiceProvider;

public class AclProjectsHelperImpl implements AclProjectsHelper {

    private final RepositoryAclServiceProvider aclServiceProvider;
    private final boolean allowProjectCreateDelete;

    public AclProjectsHelperImpl(RepositoryAclServiceProvider aclServiceProvider,
                                 boolean allowProjectCreateDelete) {
        this.aclServiceProvider = aclServiceProvider;
        this.allowProjectCreateDelete = allowProjectCreateDelete;
    }

    @Override
    public boolean hasPermission(AProject project, Permission permission) {
        if (project instanceof UserWorkspaceProject && ((UserWorkspaceProject) project).isLocalOnly()) {
            // its local project, no need to check permissions
            return true;
        }
        RepositoryAclService aclService = project instanceof ADeploymentProject
                ? aclServiceProvider.getDeployConfigRepoAclService()
                : aclServiceProvider.getDesignRepoAclService();
        if (permission.getMask() == BasePermission.DELETE.getMask()) {
            if (!allowProjectCreateDelete) {
                return false;
            }
            return aclService.isGranted(project, true, BasePermission.DELETE);
        }
        return aclService.isGranted(project, List.of(permission));
    }

    @Override
    public boolean hasCreateProjectPermission(String repoId) {
        if (!allowProjectCreateDelete) {
            return false;
        }
        RepositoryAclService aclService = aclServiceProvider.getDesignRepoAclService();
        return aclService.isGranted(repoId, null, List.of(BasePermission.CREATE));
    }
}
