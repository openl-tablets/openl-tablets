package org.openl.rules.repository;

import java.util.Collection;
import java.util.List;

import org.openl.rules.repository.exceptions.RRepositoryException;

public interface RDeploymentDescriptorProject extends RCommonProject {
    public RVersion getActiveVersion();
    public List<RVersion> getVersionHistory() throws RRepositoryException;

    /**
     * Gets collection of descriptors for projects that are included in
     * this deployment configuration.
     * 
     * @return project descriptors
     */
    Collection<RProjectDescriptor> getProjectDescriptors();
    void setProjectDescriptors(Collection<RProjectDescriptor> projectDescriptors) throws RRepositoryException;
    
    RProjectDescriptor createProjectDescriptor(String name) throws RRepositoryException;

    RDeploymentDescriptorProject getProjectVersion(CommonVersion version) throws RRepositoryException;
}
