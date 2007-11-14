package org.openl.rules.workspace.abstracts;

import java.util.Collection;

public interface DeploymentDescriptorProject {
    String getName();
    
    Collection<ProjectDescriptor> getProjectDescriptors();
    void setProjectDescriptors(Collection<ProjectDescriptor> projectDescriptors) throws ProjectException;
    
    ProjectDescriptor createProjectDescriptor(String name) throws ProjectException;

    void update() throws ProjectException;
}
