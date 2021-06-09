package org.openl.rules.ruleservice.storelogdata;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

public class StoreLogDataManagerFactoryBean implements FactoryBean<SimpleStoreLogDataManager>, ApplicationContextAware {

    private final Logger log = LoggerFactory.getLogger(StoreLogDataManagerFactoryBean.class);

    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    @Override
    public SimpleStoreLogDataManager getObject() {
        Map<String, StoreLogDataService> storeLogDataServices = applicationContext
            .getBeansOfType(StoreLogDataService.class);

        Collection<StoreLogDataService> activeStoreLogDataServices = storeLogDataServices.values()
            .stream()
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

        for (StoreLogDataService storeLoggingService : activeStoreLogDataServices) {
            log.info("Store log data service '{}' is used.", storeLoggingService.getClass().getTypeName());
        }
        return new SimpleStoreLogDataManager(activeStoreLogDataServices);
    }

    @Override
    public Class<?> getObjectType() {
        return SimpleStoreLogDataManager.class;
    }

}
