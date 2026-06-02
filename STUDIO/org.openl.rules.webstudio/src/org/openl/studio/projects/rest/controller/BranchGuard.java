package org.openl.studio.projects.rest.controller;

import java.util.Objects;

import org.openl.rules.project.abstraction.RulesProject;
import org.openl.studio.common.exception.ConflictException;
import org.openl.util.StringUtils;

/**
 * Asserts that a project is currently on the requested branch.
 *
 * <p>A blank branch is ignored. The files API operates on the project's checked-out branch;
 * the {@code branch} parameter lets a caller confirm it is acting on the branch it expects,
 * consistent with the project status endpoint. Switching branches is done separately.
 *
 * @author Yury Molchan
 */
final class BranchGuard {

    private BranchGuard() {
    }

    static void requireBranch(RulesProject project, String branch) {
        if (StringUtils.isNotBlank(branch)) {
            if (!project.isSupportsBranches()) {
                throw new ConflictException("project.branch.unsupported.message");
            }
            if (!Objects.equals(branch, project.getBranch())) {
                throw new ConflictException("project.branch.mismatch.message");
            }
        }
    }
}
