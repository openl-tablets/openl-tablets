package org.openl.rules.repository.git;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

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
public record MergeConflictDetails(
        Map<String, String> diffs,
        String yourCommit,
        String theirCommit,
        String baseCommit,
        Map<String, WorkbookDiffResult> toAutoResolve
) {

    public Collection<String> getConflictedFiles() {
        return diffs.keySet();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Map<String, String> diffs;
        private String yourCommit;
        private String theirCommit;
        private String baseCommit;
        private Map<String, WorkbookDiffResult> toAutoResolve;

        private Builder() {

        }

        public Builder diffs(Map<String, String> diffs) {
            this.diffs = diffs;
            return this;
        }

        public Builder yourCommit(String yourCommit) {
            this.yourCommit = yourCommit;
            return this;
        }

        public Builder theirCommit(String theirCommit) {
            this.theirCommit = theirCommit;
            return this;
        }

        public Builder baseCommit(String baseCommit) {
            this.baseCommit = baseCommit;
            return this;
        }

        public Builder toAutoResolve(Map<String, WorkbookDiffResult> toAutoResolve) {
            this.toAutoResolve = toAutoResolve;
            return this;
        }

        public MergeConflictDetails build() {
            return new MergeConflictDetails(diffs,
                    yourCommit,
                    theirCommit,
                    baseCommit,
                    Optional.ofNullable(toAutoResolve).orElseGet(Map::of));
        }
    }
}
