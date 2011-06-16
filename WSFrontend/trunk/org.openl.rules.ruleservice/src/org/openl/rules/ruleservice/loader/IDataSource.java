package org.openl.rules.ruleservice.loader;

import java.util.List;

import org.openl.rules.common.CommonVersion;
import org.openl.rules.project.abstraction.Deployment;
/**
 * Common interface for data source 
 * @author MKamalov
 *
 */
public interface IDataSource {
    /**
     * Returns a list of all deployments in data source
     * @return a list of deployments
     */
	List<Deployment> getDeployments();

	/**
	 * Gets a deployment from data source
	 * @param deploymentName
	 * @param deploymentVersion
	 * @return deployment
	 */
	Deployment getDeployment(String deploymentName,
			CommonVersion deploymentVersion);

	/**
	 * Returns all registered data source listeners 
	 * @return list of data source listeners
	 */
	List<IDataSourceListener> getListeners();

	/**
	 * Adds a lister to data source
	 * @param dataSourceListener
	 */
	void addListener(IDataSourceListener dataSourceListener);

	/**
	 * Removes a listener from data source
	 * @param dataSourceListener
	 */
	void removeListener(IDataSourceListener dataSourceListener);
}
