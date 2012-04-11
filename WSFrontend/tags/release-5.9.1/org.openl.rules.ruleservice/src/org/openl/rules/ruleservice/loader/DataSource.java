package org.openl.rules.ruleservice.loader;

import java.util.Collection;
import java.util.List;

import org.openl.rules.common.CommonVersion;
import org.openl.rules.project.abstraction.Deployment;

/**
 * Interface for data source.
 * 
 * @author Marat Kamalov
 * 
 */
public interface DataSource {
    /**
     * Returns a list of all deployments in data source.
     * 
     * @return a list of deployments
     */
    Collection<Deployment> getDeployments();

    /**
     * Gets a deployment from data source.
     * 
     * @param deploymentName
     * @param deploymentVersion
     * @return deployment
     */
    Deployment getDeployment(String deploymentName, CommonVersion deploymentVersion);

    /**
     * Returns all registered data source listeners.
     * 
     * @return list of data source listeners
     */
    List<DataSourceListener> getListeners();

    /**
     * Adds a lister to data source.
     * 
     * @param dataSourceListener
     */
    void addListener(DataSourceListener dataSourceListener);

    /**
     * Removes a listener from data source.
     * 
     * @param dataSourceListener
     */
    void removeListener(DataSourceListener dataSourceListener);

    /**
     * Removes all listeners from data source.
     */
    void removeAllListeners();
}
