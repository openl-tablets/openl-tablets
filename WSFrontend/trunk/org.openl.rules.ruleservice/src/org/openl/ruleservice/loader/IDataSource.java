package org.openl.ruleservice.loader;

import java.util.List;

import org.openl.rules.common.CommonVersion;
import org.openl.rules.project.abstraction.Deployment;


public interface IDataSource {
    List<Deployment> getDeployments() throws DataSourceException;
    Deployment getDeployment(String deploymentName, CommonVersion deploymentVersion) throws DataSourceException;
    
    List<DataSourceListener> getListeners() throws DataSourceException;
    void addListener(DataSourceListener dataSourceListener) throws DataSourceException;
    void removeListener(DataSourceListener dataSourceListener) throws DataSourceException;
}
