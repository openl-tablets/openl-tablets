package org.openl.rules.repository.api;

import java.util.List;
import java.util.Objects;

import org.jspecify.annotations.NonNull;

/**
 * What a merge changed in the branch it merged into.
 *
 * <p>The changes compare the branch before the merge with the commit of the merge. A workbook both branches
 * changed and the merge combined by itself is listed among the merged workbooks as well. A merge that commits a
 * given resolution of its conflicts ({@link ConflictResolveData}) combines none: the resolution did.
 *
 * @param before    the revision of the branch before the merge
 * @param after     the revision of the merge commit
 * @param changes   the files the merge changed
 * @param workbooks the workbooks the merge combined from both branches by itself
 */
public record MergeOutcome(@NonNull String before,
                           @NonNull String after,
                           @NonNull List<@NonNull FileChange> changes,
                           @NonNull List<@NonNull MergedWorkbook> workbooks) {

    public MergeOutcome {
        Objects.requireNonNull(before);
        Objects.requireNonNull(after);
        changes = List.copyOf(changes);
        workbooks = List.copyOf(workbooks);
    }
}
