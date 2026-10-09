package org.openl.studio.projects.service.protection;

import java.util.List;
import java.util.function.BooleanSupplier;

import lombok.RequiredArgsConstructor;
import org.springframework.security.acls.domain.BasePermission;

import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.repository.api.BranchRepository;
import org.openl.rules.rest.acl.service.AclProjectsHelper;
import org.openl.security.acl.repository.RepositoryAclService;
import org.openl.studio.common.exception.ForbiddenException;
import org.openl.studio.common.exception.ProtectedBranchBypassRequiredException;

@RequiredArgsConstructor
public class ProtectedBranchBypassServiceImpl implements ProtectedBranchBypassService {

    private static final String BYPASS_REQUIRED_CODE = "protected.branch.bypass.required";
    private static final String PROTECTED_CODE = "protected.branch.message";

    private final AclProjectsHelper aclProjectsHelper;
    private final RepositoryAclService designRepositoryAclService;
    private final boolean enabled;

    @Override
    public boolean isBypassEligible(AProject project) {
        if (!enabled || project == null) {
            return false;
        }
        return aclProjectsHelper.hasPermission(project, BasePermission.ADMINISTRATION);
    }

    @Override
    public boolean isBypassEligible(String repoId) {
        if (!enabled || repoId == null) {
            return false;
        }
        return designRepositoryAclService.isGranted(repoId, null, List.of(BasePermission.ADMINISTRATION));
    }

    @Override
    public void requireBypassOrThrow(BranchRepository repo, String branch, AProject projectForAcl, boolean force) {
        requireBypass(repo, branch, () -> isBypassEligible(projectForAcl), force);
    }

    @Override
    public void requireBypassOrThrow(BranchRepository repo, String branch, String repoId, boolean force) {
        requireBypass(repo, branch, () -> isBypassEligible(repoId), force);
    }

    /**
     * Lets a change of a protected branch through only for a user who may bypass the protection and confirmed it.
     * The permission is asked only for a protected branch.
     */
    private static void requireBypass(BranchRepository repo, String branch, BooleanSupplier eligible, boolean force) {
        if (!repo.isBranchProtected(branch)) {
            return;
        }
        if (!eligible.getAsBoolean()) {
            throw new ForbiddenException(PROTECTED_CODE, branch);
        }
        if (!force) {
            throw new ProtectedBranchBypassRequiredException(BYPASS_REQUIRED_CODE, branch);
        }
    }

    @Override
    public boolean isProtectionEnforced(BranchRepository repo, String branch, AProject project) {
        return repo.isBranchProtected(branch) && !isBypassEligible(project);
    }
}
