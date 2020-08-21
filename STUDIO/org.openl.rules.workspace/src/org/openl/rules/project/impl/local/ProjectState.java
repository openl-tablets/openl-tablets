package org.openl.rules.project.impl.local;

import org.openl.rules.repository.api.FileData;

public interface ProjectState {
    void notifyModified();

    boolean isModified();

    void clearModifyStatus();

    void setProjectVersion(String version);

    String getProjectVersion();

    String getRepositoryId();

    void saveFileData(String repositoryId, FileData fileData);

    FileData getFileData();
}
