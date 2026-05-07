package org.openl.studio.projects.service.protection;

import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.repository.api.BranchRepository;

/**
 * Centralizes the rules for bypassing protected-branch restrictions for users with the
 * Manager role (BasePermission.ADMINISTRATION) when the global
 * {@code security.allow-bypass-protected-branches} setting is enabled.
 *
 * <p>Eligibility is computed per project (project-level grants honored, falling back to
 * repository-level via the ACL parent strategy).
 *
 * <p>Bypass is gated by an explicit {@code force=true} confirmation from the client.
 * Without {@code force}, eligible users receive a
 * {@link org.openl.studio.common.exception.ProtectedBranchBypassRequiredException} so the
 * UI can surface a destructive-action confirmation and retry.
 */
public interface ProtectedBranchBypassService {

    /**
     * @return {@code true} if the global setting is enabled AND the current user has the
     *         Manager role on the given project (or its repository, by parent strategy).
     */
    boolean isBypassEligible(RulesProject project);

    /**
     * Repository-scoped eligibility, used when no project context exists yet
     * (e.g. {@code createProjectFromZip}).
     *
     * @return {@code true} if the global setting is enabled AND the current user has
     *         {@code ADMINISTRATION} on the given repository.
     */
    boolean isBypassEligible(String repoId);

    /**
     * Central gate for protected-branch operations. Must be called before performing the
     * mutation.
     * <ul>
     *   <li>Branch is not protected → no-op.</li>
     *   <li>Protected, eligible, {@code force=true} → no-op (allow).</li>
     *   <li>Protected, eligible, {@code force=false} → throw
     *       {@link org.openl.studio.common.exception.ProtectedBranchBypassRequiredException}.</li>
     *   <li>Protected, not eligible → throw {@link org.openl.studio.common.exception.ForbiddenException}
     *       (preserves prior 403 behavior).</li>
     * </ul>
     *
     * @param repo            the branch repository hosting the target branch
     * @param branch          the target branch name
     * @param projectForAcl   the project used for the ACL Manager check
     * @param force           the explicit confirmation flag from the client
     */
    void requireBypassOrThrow(BranchRepository repo, String branch, RulesProject projectForAcl, boolean force);

    /**
     * Repository-scoped variant of
     * {@link #requireBypassOrThrow(BranchRepository, String, RulesProject, boolean)} for
     * call sites that have no project (e.g. {@code createProjectFromZip}).
     */
    void requireBypassOrThrow(BranchRepository repo, String branch, String repoId, boolean force);
}
