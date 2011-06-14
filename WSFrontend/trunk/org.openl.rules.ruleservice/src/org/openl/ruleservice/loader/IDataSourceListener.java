package org.openl.ruleservice.loader;
/**
 * Data source listener for IDataSource
 * @author MKamalov
 *
 */
public interface IDataSourceListener {
    /**
     * Executes on deployment added to data source
     */
    void onDeploymentAdded();
}