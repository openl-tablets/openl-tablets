package org.openl.studio.repositories.service;

import static org.openl.studio.common.model.Capabilities.flag;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.stereotype.Service;

import org.openl.rules.repository.api.BranchRepository;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.rest.acl.service.AclProjectsHelper;
import org.openl.security.acl.repository.AclRepositoryType;
import org.openl.security.acl.repository.RepositoryAclServiceProvider;
import org.openl.studio.projects.service.protection.ProtectedBranchBypassService;
import org.openl.studio.repositories.model.RepositoryCapabilities;

/**
 * Computes the current user's capabilities on a repository for UI action gating.
 *
 * <p>Capabilities honour both the ACL and the repository configuration (which can take precedence over
 * the ACL), so no raw role or permission set is exposed. The result is advisory: it lets the UI show or
 * hide controls. Every operation is still enforced server-side. A capability that is not granted is
 * {@code null} (omitted) rather than {@code false}.
 */
@Service
@RequiredArgsConstructor
public class RepositoryAccessService {

    private final RepositoryAclServiceProvider aclServiceProvider;
    private final AclProjectsHelper aclProjectsHelper;
    private final ProtectedBranchBypassService bypassService;

    public RepositoryCapabilities computeCapabilities(String repositoryId, AclRepositoryType type) {
        return computeCapabilities(repositoryId, type, null);
    }

    public RepositoryCapabilities computeCapabilities(Repository repository, AclRepositoryType type) {
        return computeCapabilities(repository.getId(), type, repository);
    }

    private RepositoryCapabilities computeCapabilities(String repositoryId, AclRepositoryType type, Repository repository) {
        var design = type == AclRepositoryType.DESIGN;
        return RepositoryCapabilities.builder()
                .canCreateProject(flag(design && canCreateProject(repositoryId, repository)))
                .canManage(flag(aclServiceProvider.getAclService(type.getType())
                        .isGranted(repositoryId, null, List.of(BasePermission.ADMINISTRATION))))
                .build();
    }

    private boolean canCreateProject(String repositoryId, Repository repository) {
        if (!aclProjectsHelper.hasCreateProjectPermission(repositoryId)) {
            return false;
        }
        if (repository == null || !repository.supports().branches()) {
            return true;
        }
        var branchRepository = (BranchRepository) repository;
        return !bypassService.isProtectionEnforced(branchRepository, branchRepository.getBranch(), repositoryId);
    }
}
