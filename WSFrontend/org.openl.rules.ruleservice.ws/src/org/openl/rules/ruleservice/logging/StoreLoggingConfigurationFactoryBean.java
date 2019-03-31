package org.openl.rules.ruleservice.logging;

import java.util.Map;

import org.openl.rules.ruleservice.logging.conf.StoreLoggingConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

public class StoreLoggingConfigurationFactoryBean implements FactoryBean<StoreLoggingInfoService>, ApplicationContextAware {

    private final Logger log = LoggerFactory.getLogger(StoreLoggingConfigurationFactoryBean.class);

    private ApplicationContext applicationContext;

    private boolean loggingStoreEnable = false;

    public boolean isLoggingStoreEnable() {
        return loggingStoreEnable;
    }

    public void setLoggingStoreEnable(boolean loggingStoreEnable) {
        this.loggingStoreEnable = loggingStoreEnable;
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
    public StoreLoggingInfoService getObject() throws Exception {
        if (!isLoggingStoreEnable()) {
            return null;
        }

        Map<String, Object> storeLoggingInfoServices = applicationContext
            .getBeansWithAnnotation(StoreLoggingConfiguration.class);
        if (storeLoggingInfoServices.isEmpty()) {
            log.error("Failed to load logging store service! Please, check your configuration!");
        } else {
            if (storeLoggingInfoServices.size() > 1) {
                log.error("Failed to load logging store service! More that one logging info service was found!");
                return null;
            }
            log.info("Logging store service is loaded!");
        }
        return (StoreLoggingInfoService) storeLoggingInfoServices.entrySet().iterator().next().getValue();
    }

    @Override
    public Class<?> getObjectType() {
        return StoreLoggingInfoService.class;
    }

}
