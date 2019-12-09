package org.openl.rules.common;

public interface ProjectDescriptor<T extends CommonVersion> {

    String getProjectName();

    T getProjectVersion();

}
