package org.openl.rules.ruleservice.loader;

/**
 * Data source listener for DataSource.
 *
 * @author Marat Kamalov
 *
 */
public interface DataSourceListener {
    /**
     * Executes on deployment added to data source.
     */
    void onDeploymentAdded();
}