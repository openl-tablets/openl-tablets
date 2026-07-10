package org.openl.rules.repository.api;

import java.time.Instant;

/**
 * A branch's tip commit, for display in the branches view.
 *
 * <p>The fields describe the branch tip: its author, time, message, and revision.
 */
public record BranchStatus(
        UserInfo lastCommitAuthor,
        Instant lastCommitAt,
        String lastCommitMessage,
        String lastCommitRevision) {
}
