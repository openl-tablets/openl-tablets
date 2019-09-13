package org.openl.rules.ruleservice.logging;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

public class StoreLoggingManagerFactoryBean implements FactoryBean<SimpleStoreLoggingManager>, ApplicationContextAware {

    private final Logger log = LoggerFactory.getLogger(StoreLoggingManagerFactoryBean.class);

    private ApplicationContext applicationContext;

    private boolean storeLoggingEnabled = false;

    public boolean isStoreLoggingEnabled() {
        return storeLoggingEnabled;
    }

    public void setStoreLoggingEnabled(boolean storeLoggingEnabled) {
        this.storeLoggingEnabled = storeLoggingEnabled;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    @Override
    public SimpleStoreLoggingManager getObject() throws Exception {
        if (!isStoreLoggingEnabled()) {
            return null;
        }

        Map<String, StoreLoggingService> storeLoggingServices = applicationContext
            .getBeansOfType(StoreLoggingService.class);

        Collection<StoreLoggingService> activeStoreLoggingServices = storeLoggingServices.values()
            .stream()
            .filter(Objects::nonNull)
            .filter(StoreLoggingService::isEnabled)
            .collect(Collectors.toList());

        if (activeStoreLoggingServices.isEmpty()) {
            throw new BeanInitializationException(
                "Failed to find a store logging service. Please, verify your configuration.");
        } else {
            for (StoreLoggingService storeLoggingService : activeStoreLoggingServices) {
                log.info("Logging store service '{}' is used.", storeLoggingService.getClass().getTypeName());
            }
            return new SimpleStoreLoggingManager(activeStoreLoggingServices);
        }
    }

    @Override
    public Class<?> getObjectType() {
        return SimpleStoreLoggingManager.class;
    }

}
