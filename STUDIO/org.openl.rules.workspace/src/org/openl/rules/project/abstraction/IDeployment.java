package org.openl.rules.project.abstraction;

import java.util.Collection;

import org.openl.rules.common.CommonVersion;

public interface IDeployment {

    String getDeploymentName();
    CommonVersion getCommonVersion();
    Collection<IProject> getProjects();
    IProject getProject(String name);

}
