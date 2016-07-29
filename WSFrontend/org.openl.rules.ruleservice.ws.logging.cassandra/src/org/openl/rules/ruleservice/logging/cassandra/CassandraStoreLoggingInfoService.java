package org.openl.rules.ruleservice.logging.cassandra;

import java.lang.reflect.Method;

import org.apache.cxf.jaxrs.model.OperationResourceInfo;
import org.openl.rules.ruleservice.logging.LoggingInfo;
import org.openl.rules.ruleservice.logging.LoggingInfoMapper;
import org.openl.rules.ruleservice.logging.StoreLoggingInfoService;
import org.openl.rules.ruleservice.logging.cassandra.annotation.UseEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CassandraStoreLoggingInfoService implements StoreLoggingInfoService {

    private final Logger log = LoggerFactory.getLogger(CassandraStoreLoggingInfoService.class);

    private CassandraOperations cassandraOperations;

    LoggingInfoMapper loggingInfoMapper = new LoggingInfoMapper();

    public CassandraOperations getCassandraOperations() {
        return cassandraOperations;
    }

    public void setCassandraOperations(CassandraOperations cassandraOperations) {
        this.cassandraOperations = cassandraOperations;
    }

    @Override
    public void store(LoggingInfo loggingInfo) {
        OperationResourceInfo operationResourceInfo = loggingInfo.getOperationResourceInfo();
        if (operationResourceInfo == null) {
            log.error("Operation wasn't found. Please, see previous errors.");
            return;
        }
        Method annotatedMethod = operationResourceInfo.getAnnotatedMethod();

        Object entity = null;

        if (annotatedMethod.getAnnotation(UseEntity.class) == null) {
            entity = new LoggingRecord();
        } else {
            UseEntity useCassandraEntity = (UseEntity) annotatedMethod
                .getAnnotation(UseEntity.class);
            Class<?> entityClass = useCassandraEntity.value();
            try {
                entity = entityClass.newInstance();
            } catch (Exception e) {
                log.error("Entity class instantiation fail!", e);
                return;
            }
        }

        loggingInfoMapper.map(loggingInfo, entity);
        cassandraOperations.saveAsync(entity);

    }
}
