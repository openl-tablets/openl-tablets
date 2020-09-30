package org.openl.rules.common;

public interface ProjectDescriptor<T extends CommonVersion> {

    String getRepositoryId();

    String getProjectName();

    String getPath();

    String getBranch();

    T getProjectVersion();

}
