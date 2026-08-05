package org.openl.rules.workspace.dtr;

import java.time.Instant;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

import org.jspecify.annotations.NullMarked;

import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.repository.api.BranchStatus;

/**
 * One logical design project and the branch entries that contain it.
 *
 * <p>Each entry keeps the project path and repository view verified in that branch. The home branch is the repository
 * base branch when it contains the project. Otherwise the newest branch tip wins with a stable name tie-breaker.
 *
 * <p>Filtering recomputes the home branch. This lets secured repositories expose only readable entries without
 * leaking a raw branch repository or retaining an inaccessible home branch.
 */
@NullMarked
public record BranchedProject(String name,
                              String homeBranch,
                              String baseBranch,
                              Map<String, BranchEntry> entries) {

    private static final Comparator<String> BRANCH_ORDER = String.CASE_INSENSITIVE_ORDER
            .thenComparing(Comparator.naturalOrder());

    public BranchedProject {
        Objects.requireNonNull(name);
        Objects.requireNonNull(homeBranch);
        Objects.requireNonNull(baseBranch);
        entries = Collections.unmodifiableMap(new LinkedHashMap<>(Objects.requireNonNull(entries)));
        if (!entries.containsKey(homeBranch)) {
            throw new IllegalArgumentException("The home branch must contain the project.");
        }
    }

    /**
     * Builds a logical project and chooses its deterministic home branch.
     */
    public static BranchedProject create(String name, String baseBranch, Map<String, BranchEntry> entries) {
        if (entries.isEmpty()) {
            throw new IllegalArgumentException("A logical project must have at least one branch entry.");
        }
        return new BranchedProject(name, chooseHomeBranch(baseBranch, entries), baseBranch, entries);
    }

    public BranchEntry homeEntry() {
        return Objects.requireNonNull(entries.get(homeBranch));
    }

    public Optional<BranchEntry> entry(String branch) {
        return Optional.ofNullable(entries.get(branch));
    }

    /**
     * Whether the given branch is the only one holding the project, so removing that branch removes the project.
     *
     * <p>Branch names are matched the same way the home branch is chosen: without regard to case.
     */
    public boolean heldOnlyBy(String branch) {
        return entries.keySet().stream().allMatch(held -> held.equalsIgnoreCase(branch));
    }

    /**
     * Keeps matching branch entries and recomputes the home branch.
     */
    public Optional<BranchedProject> filter(Predicate<BranchEntry> predicate) {
        var filtered = new LinkedHashMap<String, BranchEntry>();
        entries.forEach((branch, entry) -> {
            if (predicate.test(entry)) {
                filtered.put(branch, entry);
            }
        });
        return filtered.isEmpty() ? Optional.empty() : Optional.of(create(name, baseBranch, filtered));
    }

    /**
     * Replaces each project view while preserving membership and branch status.
     */
    public BranchedProject mapProjects(UnaryOperator<AProject> mapper) {
        var mapped = new LinkedHashMap<String, BranchEntry>();
        entries.forEach((branch, entry) -> mapped.put(branch,
                new BranchEntry(mapper.apply(entry.project()), entry.status())));
        return new BranchedProject(name, homeBranch, baseBranch, mapped);
    }

    private static String chooseHomeBranch(String baseBranch, Map<String, BranchEntry> entries) {
        var actualBaseBranch = entries.keySet()
                .stream()
                .filter(branch -> branch.equalsIgnoreCase(baseBranch))
                .findFirst();
        if (actualBaseBranch.isPresent()) {
            return actualBaseBranch.orElseThrow();
        }
        return entries.keySet()
                .stream()
                .min(Comparator
                        .comparing((String branch) -> commitTime(entries.get(branch))).reversed()
                        .thenComparing(BRANCH_ORDER))
                .orElseThrow();
    }

    private static Instant commitTime(BranchEntry entry) {
        return entry.status().lastCommitAt();
    }

    /**
     * One project view and the status of the branch whose tree contains it.
     */
    public record BranchEntry(AProject project, BranchStatus status) {
        public BranchEntry {
            Objects.requireNonNull(project);
            Objects.requireNonNull(status);
        }
    }
}
