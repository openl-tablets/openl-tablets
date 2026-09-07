package org.openl.studio.projects.model.merge;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

/**
 * Response DTO containing detailed information about merge conflicts.
 * Includes conflict groups, commit details for all sides (ours, theirs, base),
 * and a default merge message.
 *
 * @param conflictGroups list of conflict groups organized by project
 * @param fileAvailability availability of each conflicted file in all merge revisions
 * @param oursRevision revision details for "ours" side (current branch)
 * @param theirsRevision revision details for "theirs" side (merging branch)
 * @param baseRevision revision details for common ancestor (base)
 * @param defaultMessage default merge commit message
 */
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ConflictDetailsResponse(
        List<ConflictGroup> conflictGroups,
        @Parameter(description = "Availability of every conflicted file in each merge revision")
        Map<String, ConflictFileAvailability> fileAvailability,
        RevisionDetails oursRevision,
        RevisionDetails theirsRevision,
        RevisionDetails baseRevision,
        String defaultMessage
) {

    /**
     * Indicates which revisions contain a conflicted file.
     *
     * @param ours whether the current branch contains the file
     * @param theirs whether the merging branch contains the file
     * @param base whether the common ancestor contains the file
     */
    @Schema(description = "Availability of a conflicted file in each merge revision")
    public record ConflictFileAvailability(
            @Parameter(description = "Whether the current branch contains the file")
            boolean ours,
            @Parameter(description = "Whether the merging branch contains the file")
            boolean theirs,
            @Parameter(description = "Whether the common ancestor contains the file")
            boolean base
    ) {
    }

    /**
     * Details about a specific revision/commit.
     *
     * @param commit commit hash/revision identifier
     * @param branch branch name (if applicable)
     * @param author author name
     * @param modifiedAt modification timestamp
     * @param exists whether this revision contains at least one conflicted file
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
