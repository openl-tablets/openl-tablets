package org.openl.studio.projects.service.protection;

import org.openl.rules.project.abstraction.AProject;
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
    boolean isBypassEligible(AProject project);

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
     *   <li>Protected, not eligible → throw {@link org.openl.studio.common.exception.ForbiddenException}.
     *       Note: this is a behavior change for the merge endpoints, which previously returned
     *       409 with {@code project.merge.branch.protected.message}. For
     *       {@code createProjectFromZip} the 403 status matches the prior contract.</li>
     * </ul>
     *
     * @param repo            the branch repository hosting the target branch
     * @param branch          the target branch name
     * @param projectForAcl   the project used for the ACL Manager check
     * @param force           the explicit confirmation flag from the client
     */
    void requireBypassOrThrow(BranchRepository repo, String branch, AProject projectForAcl, boolean force);

    /**
     * Repository-scoped variant of
     * {@link #requireBypassOrThrow(BranchRepository, String, AProject, boolean)} for
     * call sites that have no project (e.g. {@code createProjectFromZip}).
     */
    void requireBypassOrThrow(BranchRepository repo, String branch, String repoId, boolean force);

    /**
     * Convenience predicate for legacy JSF beans gating UI actions: returns
     * {@code true} when the branch is protected AND the current user is NOT eligible to
     * bypass. JSF screens lack the async confirm-then-retry flow, so eligible users get an
     * implicit bypass (the global setting is the deliberate admin opt-in; the Manager role
     * is the per-user gate).
     *
     * @return {@code true} if the action should be blocked due to branch protection.
     */
    boolean isProtectionEnforced(BranchRepository repo, String branch, AProject project);

    /**
     * Repository-scoped variant of
     * {@link #isProtectionEnforced(BranchRepository, String, AProject)} for call sites
     * with no project context (e.g. repository-list filters).
     */
    boolean isProtectionEnforced(BranchRepository repo, String branch, String repoId);
}
