package org.openl.rules.workspace.abstracts;

import org.openl.rules.repository.CommonVersion;

public interface ProjectDescriptor {
    void delete();

    String getProjectName();

    CommonVersion getProjectVersion();

    void setProjectVersion(CommonVersion version) throws ProjectException;
}
