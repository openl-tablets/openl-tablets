package org.openl.rules.repository.api;

import java.util.List;
import java.util.Set;

import org.jspecify.annotations.NonNull;

/**
 * What a merge of one branch into another brings in, told before the merge.
 *
 * <p>The changes are the changes the source branch made since the branches parted. The files changed in both are
 * the files among them the target branch changed too: the merge either combines them or reports them as conflicts.
 * The conflicts are the files among those that may conflict: both branches changed the same parts of them.
 *
 * @param changes       the files the merge changes in the target branch
 * @param changedInBoth the paths of the files both branches changed since they parted
 * @param conflicts     the paths of the files that may conflict
 */
public record MergeScope(@NonNull List<@NonNull FileChange> changes,
                         @NonNull Set<@NonNull String> changedInBoth,
                         @NonNull Set<@NonNull String> conflicts) {

    public MergeScope {
        changes = List.copyOf(changes);
        changedInBoth = Set.copyOf(changedInBoth);
        conflicts = Set.copyOf(conflicts);
    }
}
