package org.openl.rules.ruleservice.logging.cassandra;

import java.lang.reflect.Method;

import org.openl.binding.MethodUtil;
import org.openl.rules.ruleservice.logging.StoreLoggingData;
import org.openl.rules.ruleservice.logging.StoreLoggingDataMapper;
import org.openl.rules.ruleservice.logging.StoreLoggingService;
import org.openl.rules.ruleservice.logging.cassandra.annotation.CassandraEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CassandraStoreLoggingService implements StoreLoggingService {

    private final Logger log = LoggerFactory.getLogger(CassandraStoreLoggingService.class);

    private CassandraOperations cassandraOperations;

    private StoreLoggingDataMapper storeLoggingDataMapper = new StoreLoggingDataMapper();

    private boolean enabled = true;

    public CassandraOperations getCassandraOperations() {
        return cassandraOperations;
    }

    public void setCassandraOperations(CassandraOperations cassandraOperations) {
        this.cassandraOperations = cassandraOperations;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public void save(StoreLoggingData storeLoggingData) {
        Method serviceMethod = storeLoggingData.getServiceMethod();

        Object[] entities = null;
        CassandraEntity cassandraEntity = null;
        if (serviceMethod != null) {
            cassandraEntity = serviceMethod.getAnnotation(CassandraEntity.class);
            if (cassandraEntity == null) {
                cassandraEntity = serviceMethod.getDeclaringClass().getAnnotation(CassandraEntity.class);
            }
        }

        if (cassandraEntity == null || cassandraEntity.value().length == 0) {
            entities = new DefaultCassandraEntity[] { new DefaultCassandraEntity() };
        } else {
            entities = new Object[cassandraEntity.value().length];
            int i = 0;
            for (Class<?> entityClass : cassandraEntity.value()) {
                try {
                    entities[i++] = entityClass.newInstance();
                } catch (InstantiationException | IllegalAccessException e) {
                    if (log.isErrorEnabled()) {
                        log.error(String.format(
                            "Failed to instantiate cassandra entity for '%s' method. Please, check that '%s' class is not abstact and has a default constructor.",
                            MethodUtil.printQualifiedMethodName(serviceMethod),
                            entityClass.getTypeName()), e);
                    }
                    return;
                }
            }
        }
        for (Object entity : entities) {
            try {
                storeLoggingDataMapper.map(storeLoggingData, entity);
            } catch (Exception e) {
                if (log.isErrorEnabled()) {
                    log.error(String.format("Failed to map '%s' cassandra entity for '%s' method.",
                        entity.getClass().getTypeName(),
                        MethodUtil.printQualifiedMethodName(serviceMethod)), e);
                }
                return;
            }
        }
        for (Object entity : entities) {
            if (entity != null) {
                try {
                    cassandraOperations.saveAsync(entity);
                } catch (Exception e) {
                    // Continue the loop if exception occurs
                    log.error("Failed on cassandra entity save operation.", e);
                }
            }
        }
    }
}
