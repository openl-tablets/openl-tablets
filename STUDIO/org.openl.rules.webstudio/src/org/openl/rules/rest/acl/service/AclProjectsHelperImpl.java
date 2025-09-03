package org.openl.rules.rest.acl.service;

import java.util.Collection;
import java.util.List;

import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.acls.model.Permission;

import org.openl.rules.common.ProjectDescriptor;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.AProjectArtefact;
import org.openl.rules.project.abstraction.UserWorkspaceProject;
import org.openl.rules.webstudio.web.repository.DeploymentRequest;
import org.openl.security.acl.repository.RepositoryAclService;
import org.openl.security.acl.repository.RepositoryAclServiceProvider;
import org.openl.util.CollectionUtils;

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
        var aclService = aclServiceProvider.getDesignRepoAclService();
        if (permission.getMask() == BasePermission.DELETE.getMask()) {
            if (!allowProjectCreateDelete) {
                return false;
            }
            // if user is project owner, then check permissions on current resource
            boolean useParentStrategy = !aclService.isOwner(project);
            return aclService.isGranted(project, useParentStrategy, BasePermission.DELETE);
        }
        return aclService.isGranted(project, List.of(permission));
    }

    @Override
    public boolean hasPermission(AProjectArtefact child, Permission permission) {
        if (child instanceof AProject project) {
            return hasPermission(project, permission);
        }
        var aclService = aclServiceProvider.getDesignRepoAclService();
        // if user is project owner, then check delete permissions on current resource
        var useParentStrategy = permission.getMask() == BasePermission.DELETE.getMask() && !aclService.isOwner(child);
        return aclService.isGranted(child, useParentStrategy, permission);
    }

    @Override
    public boolean hasPermission(Collection<ProjectDescriptor> projects, Permission permission) {
        var aclService = aclServiceProvider.getDesignRepoAclService();
        return CollectionUtils.isEmpty(projects) || projects.stream()
                .anyMatch(pd -> aclService.isGranted(pd.repositoryId(), pd.path(), List.of(permission)));
    }

    @Override
    public boolean hasCreateProjectPermission(String repoId) {
        if (!allowProjectCreateDelete) {
            return false;
        }
        RepositoryAclService aclService = aclServiceProvider.getDesignRepoAclService();
        return aclService.isGranted(repoId, null, List.of(BasePermission.CREATE));
    }

    @Override
    public boolean hasCreateDeploymentPermission(String repoId) {
        var productionAclService = aclServiceProvider.getProdRepoAclService();
        return productionAclService.isGranted(repoId, null, List.of(BasePermission.CREATE));
    }

    @Override
    public boolean hasPermission(DeploymentRequest deploymentRequest, Permission permission) {
        var productionAclService = aclServiceProvider.getProdRepoAclService();
        boolean granted = productionAclService.isGranted(deploymentRequest.productionRepositoryId(), deploymentRequest.name(), List.of(permission));
        return granted && hasPermission(deploymentRequest.projectDescriptors(), BasePermission.READ);
    }
}
