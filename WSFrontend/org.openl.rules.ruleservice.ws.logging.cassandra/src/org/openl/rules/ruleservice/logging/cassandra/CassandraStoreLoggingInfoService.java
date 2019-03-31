package org.openl.rules.ruleservice.logging.cassandra;

import java.lang.reflect.Method;

import org.openl.rules.ruleservice.logging.LoggingInfo;
import org.openl.rules.ruleservice.logging.LoggingInfoMapper;
import org.openl.rules.ruleservice.logging.StoreLoggingInfoService;
import org.openl.rules.ruleservice.logging.cassandra.annotation.UseEntity;
import org.openl.rules.ruleservice.logging.conf.StoreLoggingConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@StoreLoggingConfiguration
public class CassandraStoreLoggingInfoService implements StoreLoggingInfoService {

    private final Logger log = LoggerFactory.getLogger(CassandraStoreLoggingInfoService.class);

    private CassandraOperations cassandraOperations;

    private LoggingInfoMapper loggingInfoMapper = new LoggingInfoMapper();

    public CassandraOperations getCassandraOperations() {
        return cassandraOperations;
    }

    public void setCassandraOperations(CassandraOperations cassandraOperations) {
        this.cassandraOperations = cassandraOperations;
    }

    @Override
    public void store(LoggingInfo loggingInfo) {
        Method serviceMethod = loggingInfo.getServiceMethod();
        if (serviceMethod == null) {
            log.error("Service method has not been found! Please, see previous errors.");
            return;
        }

        Object entity = null;

        UseEntity useCassandraEntity = serviceMethod.getAnnotation(UseEntity.class);
        if (useCassandraEntity == null) {
            useCassandraEntity = serviceMethod.getDeclaringClass().getAnnotation(UseEntity.class);
        }

        if (useCassandraEntity == null) {
            entity = new LoggingRecord();
        } else {
            Class<?> entityClass = useCassandraEntity.value();
            try {
                entity = entityClass.newInstance();
            } catch (Exception e) {
                log.error(String.format("Failed to instantiate entity class '%s'!", entityClass.getSimpleName()), e);
                return;
            }
        }

        loggingInfoMapper.map(loggingInfo, entity);
        cassandraOperations.saveAsync(entity);

    }
}
