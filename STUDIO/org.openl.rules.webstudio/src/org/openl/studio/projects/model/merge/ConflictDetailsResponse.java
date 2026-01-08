package org.openl.studio.projects.model.merge;

import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Response DTO containing detailed information about merge conflicts.
 * Includes conflict groups, commit details for all sides (ours, theirs, base),
 * and a default merge message.
 *
 * @param conflictGroups list of conflict groups organized by project
 * @param oursRevision revision details for "ours" side (current branch)
 * @param theirsRevision revision details for "theirs" side (merging branch)
 * @param baseRevision revision details for common ancestor (base)
 * @param defaultMessage default merge commit message
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ConflictDetailsResponse(
        List<ConflictGroup> conflictGroups,
        RevisionDetails oursRevision,
        RevisionDetails theirsRevision,
        RevisionDetails baseRevision,
        String defaultMessage
) {

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private List<ConflictGroup> conflictGroups;
        private RevisionDetails oursRevision;
        private RevisionDetails theirsRevision;
        private RevisionDetails baseRevision;
        private String defaultMessage;

        private Builder() {
        }

        public Builder conflictGroups(List<ConflictGroup> conflictGroups) {
            this.conflictGroups = conflictGroups;
            return this;
        }

        public Builder oursRevision(RevisionDetails oursRevision) {
            this.oursRevision = oursRevision;
            return this;
        }

        public Builder theirsRevision(RevisionDetails theirsRevision) {
            this.theirsRevision = theirsRevision;
            return this;
        }

        public Builder baseRevision(RevisionDetails baseRevision) {
            this.baseRevision = baseRevision;
            return this;
        }

        public Builder defaultMessage(String defaultMessage) {
            this.defaultMessage = defaultMessage;
            return this;
        }

        public ConflictDetailsResponse build() {
            return new ConflictDetailsResponse(
                    conflictGroups,
                    oursRevision,
                    theirsRevision,
                    baseRevision,
                    defaultMessage
            );
        }
    }

    /**
     * Details about a specific revision/commit.
     *
     * @param commit commit hash/revision identifier
     * @param branch branch name (if applicable)
     * @param author author name
     * @param modifiedAt modification timestamp
     * @param exists whether this revision exists (file may not exist in all revisions)
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record RevisionDetails(
            String commit,
            String branch,
            String author,
            Instant modifiedAt,
            boolean exists
    ) {

        public static RevisionBuilder builder() {
            return new RevisionBuilder();
        }

        public static class RevisionBuilder {
            private String commit;
            private String branch;
            private String author;
            private Instant modifiedAt;
            private boolean exists = true;

            private RevisionBuilder() {
            }

            public RevisionBuilder commit(String commit) {
                this.commit = commit;
                return this;
            }

            public RevisionBuilder branch(String branch) {
                this.branch = branch;
                return this;
            }

            public RevisionBuilder author(String author) {
                this.author = author;
                return this;
            }

            public RevisionBuilder modifiedAt(Instant modifiedAt) {
                this.modifiedAt = modifiedAt;
                return this;
            }

            public RevisionBuilder exists(boolean exists) {
                this.exists = exists;
                return this;
            }

            public RevisionDetails build() {
                return new RevisionDetails(commit, branch, author, modifiedAt, exists);
            }
        }

        /**
         * Creates a RevisionDetails for a non-existent revision.
         */
        public static RevisionDetails notExists(String commit, String branch) {
            return builder()
                    .commit(commit)
                    .branch(branch)
                    .exists(false)
                    .build();
        }

        /**
         * Creates a RevisionDetails for an existing revision.
         */
        public static RevisionDetails of(String commit, String branch, String author, Instant modifiedAt) {
            return builder()
                    .commit(commit)
                    .branch(branch)
                    .author(author)
                    .modifiedAt(modifiedAt)
                    .exists(true)
                    .build();
        }
    }
}
