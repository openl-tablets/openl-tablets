package org.openl.rules.ruleservice.loader;

/**
 * Data source listener for IDataSource
 * 
 * @author Marat Kamalov
 * 
 */
public interface DataSourceListener {
    /**
     * Executes on deployment added to data source
     */
    void onDeploymentAdded();
}