package org.openl.studio.repositories.service;

import static org.openl.studio.common.model.Capabilities.flag;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.stereotype.Service;

import org.openl.rules.repository.api.Repository;
import org.openl.rules.rest.acl.service.AclProjectsHelper;
import org.openl.security.acl.repository.AclRepositoryType;
import org.openl.security.acl.repository.RepositoryAclServiceProvider;
import org.openl.studio.repositories.model.RepositoryCapabilities;

/**
 * Computes the current user's capabilities on a repository for UI action gating.
 *
 * <p>Capabilities expose effective ACL grants without exposing a raw role or permission set. Branch
 * protection is checked against the target branch by the operation, because creating in another existing
 * or new branch can remain permitted when the configured branch is protected. The result is advisory:
 * every operation is still enforced server-side.
 */
@Service
@RequiredArgsConstructor
public class RepositoryAccessService {

    private final RepositoryAclServiceProvider aclServiceProvider;
    private final AclProjectsHelper aclProjectsHelper;

    public RepositoryCapabilities computeCapabilities(String repositoryId, AclRepositoryType type) {
        var design = type == AclRepositoryType.DESIGN;
        return RepositoryCapabilities.builder()
                .canCreateProject(flag(design && aclProjectsHelper.hasCreateProjectPermission(repositoryId)))
                .canManage(flag(aclServiceProvider.getAclService(type.getType())
                        .isGranted(repositoryId, null, List.of(BasePermission.ADMINISTRATION))))
                .build();
    }

    public RepositoryCapabilities computeCapabilities(Repository repository, AclRepositoryType type) {
        return computeCapabilities(repository.getId(), type);
    }
}
