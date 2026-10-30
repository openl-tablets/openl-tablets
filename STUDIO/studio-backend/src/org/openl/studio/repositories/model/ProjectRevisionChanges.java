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
 * @param base     the revision compared against, or {@code null} when there is none
 * @param revision the revision whose changes are told
 * @param files    the changed files
 */
@Schema(description = "The files of the project one revision changed against another")
public record ProjectRevisionChanges(
        @Parameter(description = "The revision compared against, as the history of the project tells it: the one asked for, or else the revision the changes were made on. Absent when there is none, as for the first revision of the project; then every file is added")
        @Nullable ProjectRevision base,

        @Parameter(description = "The revision whose changes are told, as the history of the project tells it")
        ProjectRevision revision,

        @Parameter(description = "The changed files, ordered by path")
        List<ChangedFile> files
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
