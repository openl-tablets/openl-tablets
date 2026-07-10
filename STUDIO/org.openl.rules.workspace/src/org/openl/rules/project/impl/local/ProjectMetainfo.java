package org.openl.rules.project.impl.local;

import java.util.Map;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Metainfo of one project in the user workspace.
 *
 * <p>The record is a snapshot of the last synchronization with the source repository: the link to the
 * repository project and the per-file baselines captured right after the project was opened or saved.
 * A project created locally has the {@code local} repository id and no revision details.
 *
 * <p>The revision details are optional. The link is complete when the version and the modification time
 * are known; the author is display metadata and may be absent.
 *
 * @author Yury Molchan
 */
@NullMarked
public record ProjectMetainfo(String repositoryId,
                              @Nullable String pathInRepository,
                              @Nullable String branch,
                              @Nullable String version,
                              @Nullable String author,
                              @Nullable Long modifiedAt,
                              @Nullable Long size,
                              @Nullable String comment,
                              Map<String, FileBaseline> files) {

    public ProjectMetainfo {
        files = Map.copyOf(files);
    }

    /**
     * Baseline of one project file captured at the last synchronization.
     *
     * <p>The unique id is the file revision id in the source repository and is absent when the repository
     * does not provide one. The size and the modification time are the values of the local copy right
     * after the synchronization; a mismatch with the actual file means the file was changed locally.
     */
    public record FileBaseline(@Nullable String uniqueId, long size, long modifiedAt) {
    }

    /**
     * Returns whether the link to the repository project is complete enough to reconstruct the revision.
     */
    public boolean hasRevision() {
        return version != null && modifiedAt != null;
    }

    /**
     * Returns a copy of this metainfo relinked to another repository.
     */
    public ProjectMetainfo withRepositoryId(String repositoryId) {
        return new ProjectMetainfo(repositoryId, pathInRepository, branch, version, author, modifiedAt, size,
                comment, files);
    }

    /**
     * Returns a copy of this metainfo with the given file baselines.
     */
    public ProjectMetainfo withFiles(Map<String, FileBaseline> files) {
        return new ProjectMetainfo(repositoryId, pathInRepository, branch, version, author, modifiedAt, size,
                comment, files);
    }
}
