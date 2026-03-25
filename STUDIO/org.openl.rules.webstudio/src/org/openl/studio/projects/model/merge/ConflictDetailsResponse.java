package org.openl.studio.projects.model.merge;

import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

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
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ConflictDetailsResponse(
        List<ConflictGroup> conflictGroups,
        RevisionDetails oursRevision,
        RevisionDetails theirsRevision,
        RevisionDetails baseRevision,
        String defaultMessage
) {

    /**
     * Details about a specific revision/commit.
     *
     * @param commit commit hash/revision identifier
     * @param branch branch name (if applicable)
     * @param author author name
     * @param modifiedAt modification timestamp
     * @param exists whether this revision exists (file may not exist in all revisions)
     */
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record RevisionDetails(
            String commit,
            String branch,
            String author,
            Instant modifiedAt,
            boolean exists
    ) {

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
