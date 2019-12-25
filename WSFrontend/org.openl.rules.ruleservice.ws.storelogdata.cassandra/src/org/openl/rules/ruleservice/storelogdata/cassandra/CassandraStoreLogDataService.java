package org.openl.rules.ruleservice.storelogdata.cassandra;

import java.lang.reflect.Method;

import org.apache.commons.lang3.StringUtils;
import org.openl.binding.MethodUtil;
import org.openl.rules.ruleservice.storelogdata.StoreLogData;
import org.openl.rules.ruleservice.storelogdata.StoreLogDataMapper;
import org.openl.rules.ruleservice.storelogdata.StoreLogDataService;
import org.openl.rules.ruleservice.storelogdata.cassandra.annotation.StoreLogDataToCassandra;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CassandraStoreLogDataService implements StoreLogDataService {

    private final Logger log = LoggerFactory.getLogger(CassandraStoreLogDataService.class);

    private CassandraOperations cassandraOperations;

    private StoreLogDataMapper storeLogDataMapper = new StoreLogDataMapper();

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
    public void save(StoreLogData storeLogData) {
        Object[] entities;

        StoreLogDataToCassandra storeLogDataToCassandraAnnotation = storeLogData.getServiceClass()
            .getAnnotation(StoreLogDataToCassandra.class);

        Method serviceMethod = storeLogData.getServiceMethod();
        if (serviceMethod != null && serviceMethod.isAnnotationPresent(StoreLogDataToCassandra.class)) {
            storeLogDataToCassandraAnnotation = serviceMethod.getAnnotation(StoreLogDataToCassandra.class);
        }

        if (storeLogDataToCassandraAnnotation == null) {
            return;
        }

        if (storeLogDataToCassandraAnnotation.value().length == 0) {
            entities = new DefaultCassandraEntity[] { new DefaultCassandraEntity() };
        } else {
            entities = new Object[storeLogDataToCassandraAnnotation.value().length];
            int i = 0;
            for (Class<?> entityClass : storeLogDataToCassandraAnnotation.value()) {
                if (StoreLogDataToCassandra.DEFAULT.class.equals(entityClass)) {
                    entities[i] = new DefaultCassandraEntity();
                } else {
                    try {
                        entities[i] = entityClass.newInstance();
                    } catch (InstantiationException | IllegalAccessException e) {
                        if (log.isErrorEnabled()) {
                            log.error(String.format(
                                "Failed to instantiate cassandra entity%s. Please, check that class '%s' is not abstract and has a default constructor.",
                                serviceMethod != null ? " for method '" + MethodUtil
                                    .printQualifiedMethodName(serviceMethod) + "'" : StringUtils.EMPTY,
                                entityClass.getTypeName()), e);
                        }
                        return;
                    }
                }
                i++;
            }
        }
        for (Object entity : entities) {
            try {
                storeLogDataMapper.map(storeLogData, entity);
            } catch (Exception e) {
                if (log.isErrorEnabled()) {
                    if (serviceMethod != null) {
                        log.error(String.format("Failed to map '%s' cassandra entity for method '%s'.",
                            entity.getClass().getTypeName(),
                            MethodUtil.printQualifiedMethodName(serviceMethod)), e);
                    } else {
                        log.error(
                            String.format("Failed to map '%s' cassandra entity.", entity.getClass().getTypeName()),
                            e);
                    }
                }
                return;
            }
        }
        for (Object entity : entities) {
            if (entity != null) {
                try {
                    cassandraOperations.save(entity);
                } catch (Exception e) {
                    // Continue the loop if exception occurs
                    log.error("Failed on cassandra entity save operation.", e);
                }
            }
        }
    }
}
