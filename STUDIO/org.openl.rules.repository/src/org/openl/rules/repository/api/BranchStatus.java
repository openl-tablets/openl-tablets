package org.openl.rules.repository.api;

import java.time.Instant;

/**
 * A branch as the repository reports it.
 *
 * <p>Most fields describe the branch tip: its author, time, message, and revision.
 *
 * <p>Protection comes from the repository configuration rather than from the branch content. It stays the same
 * whatever is pushed to the branch, until the repository is reconfigured.
 */
public record BranchStatus(
        UserInfo lastCommitAuthor,
        Instant lastCommitAt,
        String lastCommitMessage,
        String lastCommitRevision,
        boolean protectedBranch) {
}
