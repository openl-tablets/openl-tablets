package org.openl.rules.workspace.abstracts;

import java.util.Collection;

import org.openl.rules.repository.CommonVersion;

public interface DeploymentDescriptorProject {
    String getName();
    
    Collection<ProjectDescriptor> getProjectDescriptors();
    void setProjectDescriptors(Collection<ProjectDescriptor> projectDescriptors) throws ProjectException;
    
    ProjectDescriptor addProjectDescriptor(String name, CommonVersion version) throws ProjectException;
}
