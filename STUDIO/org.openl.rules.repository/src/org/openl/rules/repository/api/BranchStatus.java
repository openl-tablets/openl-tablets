package org.openl.rules.repository.api;

import java.time.Instant;

/**
 * A branch's position relative to another branch it is compared to, plus its tip commit, for display in
 * the branches view.
 *
 * <p>{@code commitsAhead} is the number of commits on the branch that are not on the compared-to branch;
 * {@code commitsBehind} is the number on the compared-to branch that are not on the branch. Both are 0
 * when a branch is compared to itself. The last-commit fields describe the branch tip.
 */
public record BranchStatus(
        int commitsAhead,
        int commitsBehind,
        UserInfo lastCommitAuthor,
        Instant lastCommitAt,
        String lastCommitMessage,
        String lastCommitRevision) {
}
