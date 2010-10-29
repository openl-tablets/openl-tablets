package org.openl.rules.repository;

import java.util.Collection;
import java.util.List;

import org.openl.rules.repository.exceptions.RRepositoryException;

public interface RDeploymentDescriptorProject extends RCommonProject {

    RProjectDescriptor createProjectDescriptor(String name) throws RRepositoryException;

    RVersion getActiveVersion();

    /**
     * Gets collection of descriptors for projects that are included in this
     * deployment configuration.
     *
     * @return project descriptors
     */
    Collection<RProjectDescriptor> getProjectDescriptors();

    RDeploymentDescriptorProject getProjectVersion(CommonVersion version) throws RRepositoryException;

    List<RVersion> getVersionHistory() throws RRepositoryException;

    void setProjectDescriptors(Collection<RProjectDescriptor> projectDescriptors) throws RRepositoryException;
}
