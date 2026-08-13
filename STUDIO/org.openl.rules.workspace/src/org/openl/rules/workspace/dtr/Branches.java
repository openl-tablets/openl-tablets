package org.openl.rules.workspace.dtr;

import java.util.Collection;
import java.util.Comparator;
import java.util.Optional;
import java.util.function.Function;

import org.jspecify.annotations.NullMarked;

import org.openl.rules.repository.api.BranchStatus;

/**
 * The branches of one repository: the order they are listed in, and the one a project is represented by.
 *
 * <p>The home branch depends on how authoritative a branch is, not on how recently someone pushed to it: the
 * repository base branch when it holds the project, then a branch the repository protects, then the newest tip,
 * and the branch name settles every tie. A repository that protects no branch keeps the plain recency rule.
 */
@NullMarked
final class Branches {

    /**
     * Branch names in a stable order: case-insensitive first, so look-alike names stay together, then exact.
     */
    static final Comparator<String> ORDER = String.CASE_INSENSITIVE_ORDER.thenComparing(Comparator.naturalOrder());

    /**
     * The most authoritative branch first: a protected branch, then the branch whose tip is newest.
     */
    private static final Comparator<BranchStatus> BY_AUTHORITY = Comparator
            .comparing(BranchStatus::protectedBranch)
            .thenComparing(BranchStatus::lastCommitAt)
            .reversed();

    private Branches() {
    }

    /**
     * Chooses the branch a project is represented by among the branches that hold it.
     *
     * @param branches   branches whose current tree holds the project, never empty
     * @param baseBranch configured repository base branch
     * @param statusOf   the status of a given branch
     */
    static String home(Collection<String> branches, String baseBranch, Function<String, BranchStatus> statusOf) {
        return actual(branches, baseBranch).orElseGet(() -> branches.stream()
                .min(Comparator.comparing(statusOf, BY_AUTHORITY).thenComparing(ORDER))
                .orElseThrow());
    }

    /**
     * The actual ref matching a configured branch name, whatever casing the configuration used.
     */
    static Optional<String> actual(Collection<String> branches, String configured) {
        return branches.stream().filter(branch -> branch.equalsIgnoreCase(configured)).findFirst();
    }
}
