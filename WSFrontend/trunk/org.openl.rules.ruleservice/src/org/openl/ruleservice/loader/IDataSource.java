package org.openl.ruleservice.loader;

import java.util.List;

import org.openl.rules.common.CommonVersion;
import org.openl.rules.project.abstraction.Deployment;

public interface IDataSource {
	List<Deployment> getDeployments();

	Deployment getDeployment(String deploymentName,
			CommonVersion deploymentVersion);

	List<DataSourceListener> getListeners();

	void addListener(DataSourceListener dataSourceListener);

	void removeListener(DataSourceListener dataSourceListener);
}
