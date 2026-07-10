package org.openl.rules.project.impl.local;

import org.openl.rules.repository.api.FileData;

public interface ProjectState {
    void notifyModified();

    boolean isModified();

    void clearModifyStatus();

    void setProjectVersion(String version);

    String getProjectVersion();

    String getRepositoryId();

    /**
     * Stores the link between the local project copy and its source repository project.
     *
     * <p>The link identifies the repository, the path in it, and the checked out version. The author and
     * other revision details are optional display metadata. The link is written whenever the version and
     * the modification time are known; a missing author does not prevent it.
     */
    void saveFileData(String repositoryId, FileData fileData);

    /**
     * Returns the stored link between the local project copy and its source repository project.
     *
     * <p>Returns {@code null} when the link is absent or lacks the essential data: the version or the
     * modification time. A stored link without an author is returned with a {@code null} author.
     */
    FileData getFileData();

}
