package org.openl.rules.ruleservice.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

public class StoreLoggingConfigurationFactoryBean implements FactoryBean<LoggingInfoStoringService>, ApplicationContextAware, InitializingBean {

    private final Logger log = LoggerFactory.getLogger(StoreLoggingConfigurationFactoryBean.class);

    private static final String CASSANDRA_STORING_SERVICE_BEAN_NAME = "cassandraLoggingInfoStoreService";

    private static final String ELASTICSEARCH_STORING_SERVICE_BEAN_NAME = "elasticSearchLoggingInfoStoreService";

    private static final String CASSANDRA_TYPE = "cassandra";

    private static final String ELASTICSEARCH_TYPE = "elasticsearch";

    private ApplicationContext applicationContext;

    private String type = CASSANDRA_TYPE;

    private boolean loggingStoreEnable = false;

    public boolean isLoggingStoreEnable() {
        return loggingStoreEnable;
    }

    public void setLoggingStoreEnable(boolean loggingStoreEnable) {
        this.loggingStoreEnable = loggingStoreEnable;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public boolean isSingleton() {
        return false;
    }

    @Override
    public LoggingInfoStoringService getObject() throws Exception {
        if (!isLoggingStoreEnable()) {
            return null;
        }
        if (CASSANDRA_TYPE.equalsIgnoreCase(getType())) {
            log.info("Cassandra logging store is enabled!");
            return applicationContext.getBean(CASSANDRA_STORING_SERVICE_BEAN_NAME, LoggingInfoStoringService.class);
        } else if (ELASTICSEARCH_TYPE.equalsIgnoreCase(getType())) {
            log.info("Elastic Search logging store is enabled!");
            LoggingInfoStoringService loggingInfoStoringService = applicationContext.getBean(ELASTICSEARCH_STORING_SERVICE_BEAN_NAME, LoggingInfoStoringService.class);
            if (loggingInfoStoringService == null){
                log.error("Elastic Search logging store wasn't configured! Please, refer to OpenL documentation.");
            }
            return loggingInfoStoringService;
        } else {
            return null;
        }
    }

    @Override
    public Class<?> getObjectType() {
        return LoggingInfoStoringService.class;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        if (!(CASSANDRA_TYPE.equalsIgnoreCase(getType())) && !(ELASTICSEARCH_TYPE.equalsIgnoreCase(getType()))) {
            throw new IllegalArgumentException(
                "Property 'type' is required! Supported value is '" + CASSANDRA_TYPE + "','" + ELASTICSEARCH_TYPE + "!");
        }
    }

}
