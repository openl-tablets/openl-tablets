package org.openl.ruleservice.management;

import org.openl.ruleservice.RuleService;
import org.openl.ruleservice.loader.DataSourceListener;

public class ServiceManager implements DataSourceListener{
    private RuleService ruleService;
    private IServiceConfigurer serviceConfigurer; 

    public void onDeploymentAdded() {
        // TODO Auto-generated method stub
    }
    
}
