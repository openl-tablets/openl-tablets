package org.openl.rules.repository;

import org.openl.rules.common.ProjectDescriptor;
import org.openl.rules.repository.exceptions.RRepositoryException;

public interface RProjectDescriptor extends ProjectDescriptor<RVersion> {
    /**
     * Gets name of rules project.
     *
     * @return name of project
     */
    @Override
    String getProjectName();

    /**
     * Gets version of rules project.
     *
     * @return version of project
     */
    @Override
    RVersion getProjectVersion();

    @Override
    void setProjectVersion(RVersion version) throws RRepositoryException;
}
