package org.openl.rules.common;

public interface ProjectDescriptor {

    String repositoryId();

    String projectName();

    String path();

    String branch();

    CommonVersion projectVersion();

}
