package org.openl.rules.repository;

import java.util.Collection;

import org.openl.rules.repository.exceptions.RModifyException;
import org.openl.rules.repository.exceptions.RRepositoryException;

public interface RDeploymentDescriptorProject extends RCommonProject {
    /**
     * Gets collection of descriptors for projects that are included in
     * this deployment configuration.
     * 
     * @return project descriptors
     */
    Collection<RProjectDescriptor> getProjectDescriptors();
    void setProjectDescriptors(Collection<RProjectDescriptor> projectDescriptors) throws RModifyException;
    
    RProjectDescriptor createProjectDescriptor(String name) throws RRepositoryException;
}
