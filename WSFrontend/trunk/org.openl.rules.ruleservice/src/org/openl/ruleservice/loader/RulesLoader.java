package org.openl.ruleservice.loader;

import java.util.List;

import org.openl.rules.common.CommonVersion;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.Deployment;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.resolving.RulesProjectResolver;
import org.openl.ruleservice.ServiceDescription;

public class RulesLoader implements IRulesLoader {
    private IDataSource dataSource;
    private RulesProjectResolver projectResolver;
    //TODO private local workspace

    public IDataSource getDataSource() {
        return dataSource;
    }

    public List<Deployment> getDeployments() {
        return dataSource.getDeployments();
    }

    public Deployment getDeployemnt(String name, CommonVersion deploymentVersion) {
        return dataSource.getDeployemnt(name, deploymentVersion);
    }

    public List<AProject> getProjectsForDeployment(String name, CommonVersion deploymentVersion) {
        return dataSource.getProjectsForDeployment(name, deploymentVersion);
    }

    public List<Module> resolveModulesForProject(String deploymentName, CommonVersion deploymentVersion,
            String projectName) {
        // TODO Auto-generated method stub
        return null;
    }

    public List<Module> getModulesForService(ServiceDescription serviceDescription) {
        // TODO Auto-generated method stub
        return null;
    }

}
