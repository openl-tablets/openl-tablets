package org.openl.ruleservice.loader;

import java.util.List;

import org.openl.rules.common.CommonVersion;
import org.openl.rules.project.abstraction.Deployment;
import org.openl.rules.project.model.Module;
import org.openl.ruleservice.ServiceDescription;

public interface IRulesLoader {
	IDataSource getDataSource();

	List<Deployment> getDeployments();

	Deployment getDeployment(String deploymentName,
			CommonVersion deploymentVersion);

	List<Module> resolveModulesForProject(String deploymentName,
			CommonVersion deploymentVersion, String projectName);

	List<Module> getModulesForService(ServiceDescription serviceDescription);
}
