package org.openl.rules.workspace.abstracts;

import java.util.Collection;

import org.openl.rules.common.CommonVersion;
import org.openl.rules.common.ProjectDescriptor;
import org.openl.rules.common.ProjectException;

public interface DeploymentDescriptorProject {
    ProjectDescriptor addProjectDescriptor(String name, CommonVersion version) throws ProjectException;

    String getName();

    Collection<ProjectDescriptor> getProjectDescriptors();

    void setProjectDescriptors(Collection<ProjectDescriptor> projectDescriptors) throws ProjectException;
}
