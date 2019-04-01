package org.openl.rules.ruleservice.loader;

import java.util.Collection;

import org.openl.rules.common.CommonVersion;
import org.openl.rules.project.abstraction.Deployment;

/**
 * Interface for data source.
 *
 * @author Marat Kamalov
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
     * Adds a lister to data source.
     *
     * @param dataSourceListener
     */
    void setListener(DataSourceListener dataSourceListener);
}
