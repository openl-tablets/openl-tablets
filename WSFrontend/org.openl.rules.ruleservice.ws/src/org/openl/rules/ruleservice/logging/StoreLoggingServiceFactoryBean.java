package org.openl.rules.ruleservice.logging;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

public class StoreLoggingServiceFactoryBean implements FactoryBean<StoreLoggingService>, ApplicationContextAware {

    private final Logger log = LoggerFactory.getLogger(StoreLoggingServiceFactoryBean.class);

    private ApplicationContext applicationContext;

    private boolean loggingStoreEnabled = false;

    public boolean isLoggingStoreEnabled() {
        return loggingStoreEnabled;
    }

    public void setLoggingStoreEnabled(boolean loggingStoreEnabled) {
        this.loggingStoreEnabled = loggingStoreEnabled;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public boolean isSingleton() {
        return false;
    }

    @Override
    public StoreLoggingService getObject() throws Exception {
        if (!isLoggingStoreEnabled()) {
            return null;
        }

        Map<String, StoreLoggingService> storeLoggingServices = applicationContext
            .getBeansOfType(StoreLoggingService.class);
        if (storeLoggingServices.isEmpty()) {
            log.error("Failed to load logging store service! Please, check your configuration!");
        } else {
            if (storeLoggingServices.size() > 1) {
                log.error("Failed to load logging store service! Multiple store logging service is found!");
                return null;
            }
            log.info("Logging store service is loaded!");
        }
        return storeLoggingServices.entrySet().iterator().next().getValue();
    }

    @Override
    public Class<?> getObjectType() {
        return StoreLoggingService.class;
    }

}
