package org.openl.studio.projects.service.protection;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.security.acls.domain.BasePermission;

import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.repository.api.BranchRepository;
import org.openl.rules.rest.acl.service.AclProjectsHelper;
import org.openl.security.acl.repository.RepositoryAclService;
import org.openl.studio.common.exception.ForbiddenException;
import org.openl.studio.common.exception.ProtectedBranchBypassRequiredException;

@RequiredArgsConstructor
public class ProtectedBranchBypassServiceImpl implements ProtectedBranchBypassService {

    private static final String BYPASS_REQUIRED_CODE = "protected.branch.bypass.required";

    private final AclProjectsHelper aclProjectsHelper;
    private final RepositoryAclService designRepositoryAclService;
    private final boolean enabled;

    @Override
    public boolean isBypassEligible(RulesProject project) {
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
    public void requireBypassOrThrow(BranchRepository repo, String branch, RulesProject projectForAcl, boolean force) {
        if (!repo.isBranchProtected(branch)) {
            return;
        }
        if (!isBypassEligible(projectForAcl)) {
            throw new ForbiddenException();
        }
        if (!force) {
            throw new ProtectedBranchBypassRequiredException(BYPASS_REQUIRED_CODE, branch);
        }
    }

    @Override
    public void requireBypassOrThrow(BranchRepository repo, String branch, String repoId, boolean force) {
        if (!repo.isBranchProtected(branch)) {
            return;
        }
        if (!isBypassEligible(repoId)) {
            throw new ForbiddenException();
        }
        if (!force) {
            throw new ProtectedBranchBypassRequiredException(BYPASS_REQUIRED_CODE, branch);
        }
    }
}
