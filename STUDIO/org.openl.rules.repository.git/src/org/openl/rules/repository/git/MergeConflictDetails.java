package org.openl.rules.repository.git;

import java.util.Collection;
import java.util.Map;

import lombok.Builder;

import org.openl.rules.xls.merge.diff.WorkbookDiffResult;

/**
 * Details about a merge conflict.
 *
 * @param diffs         A map of file paths to their respective diff strings.
 * @param yourCommit    The commit hash representing your changes.
 * @param theirCommit   The commit hash representing the incoming changes.
 * @param baseCommit    The common ancestor commit hash.
 * @param toAutoResolve A map of file paths to their respective WorkbookDiffResult for files that can be auto-resolved.
 */
@Builder
public record MergeConflictDetails(
        Map<String, String> diffs,
        String yourCommit,
        String theirCommit,
        String baseCommit,
        Map<String, WorkbookDiffResult> toAutoResolve
) {

    public MergeConflictDetails {
        if (toAutoResolve == null) {
            toAutoResolve = Map.of();
        }
    }

    public Collection<String> getConflictedFiles() {
        return diffs.keySet();
    }
}
