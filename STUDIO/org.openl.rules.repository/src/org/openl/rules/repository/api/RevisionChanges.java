package org.openl.rules.repository.api;

import java.util.List;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * The files one revision changes against another.
 *
 * <p>The earlier revision is the one the comparison was made against. It is absent when there was nothing to compare
 * against, such as for the first revision of a repository: then every file is added.
 *
 * <p>Both revisions are told as the history lists them: their number, author, time and comment.
 *
 * @param from    the earlier revision, or {@code null} when there is none
 * @param to      the later revision
 * @param changes the changed files, by their paths from the root of the repository
 */
public record RevisionChanges(@Nullable FileData from,
                              @NonNull FileData to,
                              @NonNull List<@NonNull FileChange> changes) {

    public RevisionChanges {
        changes = List.copyOf(changes);
    }
}
