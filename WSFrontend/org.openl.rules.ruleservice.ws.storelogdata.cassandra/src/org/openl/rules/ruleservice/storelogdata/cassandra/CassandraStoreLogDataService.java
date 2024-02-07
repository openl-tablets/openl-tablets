package org.openl.rules.ruleservice.storelogdata.cassandra;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.openl.binding.MethodUtil;
import org.openl.rules.ruleservice.storelogdata.AbstractStoreLogDataService;
import org.openl.rules.ruleservice.storelogdata.Inject;
import org.openl.rules.ruleservice.storelogdata.StoreLogData;
import org.openl.rules.ruleservice.storelogdata.StoreLogDataException;
import org.openl.rules.ruleservice.storelogdata.StoreLogDataMapper;
import org.openl.rules.ruleservice.storelogdata.annotation.AnnotationUtils;
import org.openl.rules.ruleservice.storelogdata.cassandra.annotation.CassandraSession;
import org.openl.rules.ruleservice.storelogdata.cassandra.annotation.StoreLogDataToCassandra;
import org.openl.spring.config.ConditionalOnEnable;

@Component
@ConditionalOnEnable("ruleservice.store.logs.cassandra.enabled")
public class CassandraStoreLogDataService extends AbstractStoreLogDataService {

    @Autowired
    private CassandraOperations cassandraOperations;

    private final StoreLogDataMapper storeLogDataMapper = new StoreLogDataMapper();

    private Collection<Inject<?>> supportedInjects;

    @PostConstruct
    public void setup() {
        supportedInjects = Collections
                .singleton(new Inject<>(CassandraSession.class, (m, a) -> cassandraOperations.getCqlSession()));
    }

    @Override
    public boolean isSync(StoreLogData storeLogData) {
        StoreLogDataToCassandra storeLogDataToCassandra = AnnotationUtils
                .getAnnotationInServiceClassOrServiceMethod(storeLogData, StoreLogDataToCassandra.class);
        if (storeLogDataToCassandra != null) {
            return storeLogDataToCassandra.sync();
        }
        return false;
    }

    @Override
    public Collection<Inject<?>> additionalInjects() {
        return supportedInjects;
    }

    @Override
    protected void save(StoreLogData storeLogData, boolean sync) throws StoreLogDataException {
        StoreLogDataToCassandra storeLogDataToCassandraAnnotation = storeLogData.getServiceClass()
                .getAnnotation(StoreLogDataToCassandra.class);

        Method serviceMethod = storeLogData.getServiceMethod();
        if (serviceMethod != null && serviceMethod.isAnnotationPresent(StoreLogDataToCassandra.class)) {
            storeLogDataToCassandraAnnotation = serviceMethod.getAnnotation(StoreLogDataToCassandra.class);
        }

        if (storeLogDataToCassandraAnnotation == null) {
            return;
        }
        List<Object> entities = new ArrayList<>();
        if (storeLogDataToCassandraAnnotation.value().length == 0) {
            if (!storeLogData.isIgnorable(DefaultCassandraEntity.class)) {
                entities.add(new DefaultCassandraEntity());
            }
        } else {
            for (Class<?> entityClass : storeLogDataToCassandraAnnotation.value()) {
                if (!storeLogData.isIgnorable(entityClass)) {
                    if (StoreLogDataToCassandra.DEFAULT.class == entityClass) {
                        entities.add(new DefaultCassandraEntity());
                    } else {
                        try {
                            entities.add(entityClass.newInstance());
                        } catch (InstantiationException | IllegalAccessException e) {
                            throw new StoreLogDataException(String.format(
                                    "Failed to instantiate cassandra entity%s. Please, check that class '%s' is not abstract and has a default constructor.",
                                    serviceMethod != null ? (" for method '" + MethodUtil
                                            .printQualifiedMethodName(serviceMethod) + "'") : StringUtils.EMPTY,
                                    entityClass.getTypeName()), e);
                        }
                    }
                }
            }
        }
        for (Object entity : entities) {
            try {
                storeLogDataMapper.map(storeLogData, entity);
            } catch (Exception e) {
                if (serviceMethod != null) {
                    throw new StoreLogDataException(
                            String.format("Failed to populate cassandra entity '%s' for method '%s'.",
                                    entity.getClass().getTypeName(),
                                    MethodUtil.printQualifiedMethodName(serviceMethod)),
                            e);
                } else {
                    throw new StoreLogDataException(
                            String.format("Failed to populate cassandra entity '%s'.", entity.getClass().getTypeName()),
                            e);
                }
            }
        }
        for (Object entity : entities) {
            if (entity != null) {
                try {
                    cassandraOperations.save(entity, sync);
                } catch (Exception e) {
                    // Continue the loop if exception occurs
                    throw new StoreLogDataException("Failed on cassandra entity save operation.", e);
                }
            }
        }
    }
}
