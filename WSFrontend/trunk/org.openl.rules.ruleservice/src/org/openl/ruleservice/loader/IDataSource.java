package org.openl.ruleservice.loader;

import java.util.List;

import org.openl.rules.common.CommonVersion;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.Deployment;


public interface IDataSource {
    List<Deployment> getDeployments();
    Deployment getDeployemnt(String name, CommonVersion deploymentVersion);
    List<AProject> getProjectsForDeployment(String name, CommonVersion deploymentVersion);
    
    void addListener(DataSourceListener listener);
    void removeListener(DataSourceListener listener);
}
