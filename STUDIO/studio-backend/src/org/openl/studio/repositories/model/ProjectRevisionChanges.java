package org.openl.studio.repositories.model;

import java.util.List;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

import org.openl.studio.projects.model.project.status.ChangeType;

/**
 * The files of a project one revision changed against another.
 *
 * <p>Both revisions are told as the history of the project tells them: their number, author, time and comment.
 *
 * <p>When the base is not in the history of the revision, as a revision of another branch is not, the revision
 * both histories parted at is told too, and the files are the ones the revision changed since it, as a pull request
 * lists them. The files among them that may conflict in a merge of both sides are named too.
 *
 * @param base     the revision compared against, or {@code null} when there is none
 * @param revision the revision whose changes are told, or {@code null} for the working copy
 * @param common   the revision the histories of both sides parted at, or {@code null} when the base is in the
 *                 history of the revision
 * @param files     the changed files
 * @param conflicts the paths of the changed files that may conflict in a merge of both sides
 */
@Schema(description = "The files of the project one revision changed against another")
public record ProjectRevisionChanges(
        @Parameter(description = "The revision compared against, as the history of the project tells it: the one asked for, or else the revision the changes were made on. Absent when there is none, as for the first revision of the project; then every file is added")
        @Nullable ProjectRevision base,

        @Parameter(description = "The revision whose changes are told, as the history of the project tells it. Absent for the working copy of the project, compared with the base")
        @Nullable ProjectRevision revision,

        @Parameter(description = "The revision the histories of the base and of the revision, or of the working copy, parted at, as the history of the project tells it. Present only when the base is not in that history, as a revision of another branch is not: the files are then the ones the revision, or the working copy, changed since it, as a pull request lists them")
        @Nullable ProjectRevision common,

        @Parameter(description = "The changed files, ordered by path")
        List<ChangedFile> files,

        @Parameter(description = "Paths of the changed files that may conflict in a merge of both sides, ordered by path: both sides changed the same parts of them. Told only with the common revision. An estimate made from the saved revisions, without merging, so the changes of the working copy not saved yet are not in it")
        List<String> conflicts
) {

    /**
     * A file of the project a revision changed.
     *
     * @param path the path of the file inside the project
     * @param type what became of the file
     */
    @Schema(description = "A file of the project a revision changed")
    public record ChangedFile(
            @Parameter(description = "Path of the file inside the project, with forward slashes")
            String path,

            @Parameter(description = "What the revision did to the file")
            ChangeType type
    ) {
    }
}
