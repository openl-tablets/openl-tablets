package org.openl.rules.ruleservice.conf;

import org.springframework.beans.factory.InitializingBean;

import org.openl.rules.ruleservice.management.ServiceManager;

public final class ServiceManagerStarterBean implements InitializingBean {
    ServiceManager serviceManager;

    public void setServiceManager(ServiceManager serviceManager) {
        this.serviceManager = serviceManager;
    }

    public ServiceManager getServiceManager() {
        return serviceManager;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        serviceManager.start();
    }
}
