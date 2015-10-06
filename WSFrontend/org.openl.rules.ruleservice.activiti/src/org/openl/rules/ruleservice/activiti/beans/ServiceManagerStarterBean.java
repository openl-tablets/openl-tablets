package org.openl.rules.ruleservice.activiti.beans;

import org.openl.rules.ruleservice.management.ServiceManager;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

public final class ServiceManagerStarterBean implements InitializingBean {
    @Autowired
    ServiceManager serviceManager;
    
    @Override
    public void afterPropertiesSet() throws Exception {
        serviceManager.start();
    }
}
