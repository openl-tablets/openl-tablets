package org.openl.rules.ruleservice.storelogdata;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.openl.rules.ruleservice.storelogdata.StoreLogDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

public class StoreLogDataManagerFactoryBean implements FactoryBean<SimpleStoreLogDataManager>, ApplicationContextAware {

    private final Logger log = LoggerFactory.getLogger(StoreLogDataManagerFactoryBean.class);

    private ApplicationContext applicationContext;

    private boolean storeLogDataEnabled = false;

    public boolean isStoreLogDataEnabled() {
        return storeLogDataEnabled;
    }

    public void setStoreLogDataEnabled(boolean storeLogDataEnabled) {
        this.storeLogDataEnabled = storeLogDataEnabled;
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
    public SimpleStoreLogDataManager getObject() throws Exception {
        if (!isStoreLogDataEnabled()) {
            return null;
        }

        Map<String, StoreLogDataService> storeLogDataServices = applicationContext
            .getBeansOfType(StoreLogDataService.class);

        Collection<StoreLogDataService> activeStoreLogDataServices = storeLogDataServices.values()
            .stream()
            .filter(Objects::nonNull)
            .filter(StoreLogDataService::isEnabled)
            .collect(Collectors.toList());

        if (activeStoreLogDataServices.isEmpty()) {
            throw new BeanInitializationException(
                "Failed to find a store logging service. Please, verify your configuration.");
        } else {
            for (StoreLogDataService storeLoggingService : activeStoreLogDataServices) {
                log.info("Store log data service '{}' is used.", storeLoggingService.getClass().getTypeName());
            }
            return new SimpleStoreLogDataManager(activeStoreLogDataServices);
        }
    }

    @Override
    public Class<?> getObjectType() {
        return SimpleStoreLogDataManager.class;
    }

}
