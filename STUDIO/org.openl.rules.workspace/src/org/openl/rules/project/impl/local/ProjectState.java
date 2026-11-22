package org.openl.rules.project.impl.local;

import java.util.Map;

import org.jspecify.annotations.Nullable;

import org.openl.rules.repository.api.FileData;

/**
 * State of one project in the user workspace, backed by the per-user metainfo registry.
 */
public interface ProjectState {

    /**
     * Marks the project as locally changed.
     */
    void notifyModified();

    /**
     * Returns whether the project has local changes.
     */
    boolean isModified();

    @Nullable
    String getProjectVersion();

    @Nullable
    String getRepositoryId();

    /**
     * Stores the synchronization snapshot: the link to the repository project and the baselines of all
     * project files. Resets the local-changes state.
     */
    void saveSnapshot(String repositoryId, FileData fileData, Map<String, ProjectMetainfo.FileBaseline> baselines);

    /**
     * Returns the stored link between the local project copy and its source repository project.
     *
     * <p>Returns {@code null} when the link is absent or lacks the essential data: the version or the
     * modification time. A stored link without an author is returned with a {@code null} author.
     */
    @Nullable
    FileData getFileData();

}
