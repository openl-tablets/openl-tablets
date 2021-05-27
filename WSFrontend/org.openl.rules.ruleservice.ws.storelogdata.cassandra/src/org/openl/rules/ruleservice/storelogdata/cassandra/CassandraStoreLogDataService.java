package org.openl.rules.ruleservice.storelogdata.cassandra;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.apache.commons.lang3.StringUtils;
import org.openl.binding.MethodUtil;
import org.openl.rules.ruleservice.storelogdata.AbstractStoreLogDataService;
import org.openl.rules.ruleservice.storelogdata.Inject;
import org.openl.rules.ruleservice.storelogdata.StoreLogData;
import org.openl.rules.ruleservice.storelogdata.StoreLogDataMapper;
import org.openl.rules.ruleservice.storelogdata.annotation.AnnotationUtils;
import org.openl.rules.ruleservice.storelogdata.cassandra.annotation.CassandraSession;
import org.openl.rules.ruleservice.storelogdata.cassandra.annotation.StoreLogDataToCassandra;
import org.openl.spring.config.ConditionalOnEnable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnEnable("ruleservice.store.logs.cassandra.enabled")
public class CassandraStoreLogDataService extends AbstractStoreLogDataService {

    private static final Logger LOG = LoggerFactory.getLogger(CassandraStoreLogDataService.class);

    @Autowired
    private CassandraOperations cassandraOperations;

    private final StoreLogDataMapper storeLogDataMapper = new StoreLogDataMapper();

    public CassandraOperations getCassandraOperations() {
        return cassandraOperations;
    }

    public void setCassandraOperations(CassandraOperations cassandraOperations) {
        this.cassandraOperations = cassandraOperations;
    }

    private volatile Collection<Inject<?>> supportedInjects;

    @Override
    protected boolean isSync(StoreLogData storeLogData) {
        StoreLogDataToCassandra storeLogDataToCassandra = AnnotationUtils
            .getAnnotationInServiceClassOrServiceMethod(storeLogData, StoreLogDataToCassandra.class);
        if (storeLogDataToCassandra != null) {
            return storeLogDataToCassandra.sync();
        }
        return false;
    }

    @Override
    public Collection<Inject<?>> additionalInjects() {
        if (supportedInjects == null) {
            synchronized (this) {
                if (supportedInjects == null) {
                    Collection<Inject<?>> injects = new ArrayList<>();
                    injects.add(new Inject<>(CassandraSession.class, cassandraOperations::getCqlSession));
                    supportedInjects = Collections.unmodifiableCollection(injects);
                }
            }
        }
        return supportedInjects;
    }

    @Override
    protected void save(StoreLogData storeLogData, boolean sync) {
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
                if (StoreLogDataToCassandra.DEFAULT.class == entityClass) {
                    entities[i] = new DefaultCassandraEntity();
                } else {
                    try {
                        entities[i] = entityClass.newInstance();
                    } catch (InstantiationException | IllegalAccessException e) {
                        if (LOG.isErrorEnabled()) {
                            LOG.error(String.format(
                                "Failed to instantiate cassandra entity%s. Please, check that class '%s' is not abstract and has a default constructor.",
                                serviceMethod != null ? (" for method '" + MethodUtil
                                    .printQualifiedMethodName(serviceMethod) + "'") : StringUtils.EMPTY,
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
                if (LOG.isErrorEnabled()) {
                    if (serviceMethod != null) {
                        LOG.error("Failed to populate cassandra entity '{}' for method '{}'.",
                            entity.getClass().getTypeName(),
                            MethodUtil.printQualifiedMethodName(serviceMethod),
                            e);
                    } else {
                        LOG.error("Failed to populate cassandra entity '{}'.", entity.getClass().getTypeName(), e);
                    }
                }
                return;
            }
        }
        for (Object entity : entities) {
            if (entity != null) {
                try {
                    cassandraOperations.save(entity, sync);
                } catch (Exception e) {
                    // Continue the loop if exception occurs
                    LOG.error("Failed on cassandra entity save operation.", e);
                }
            }
        }
    }
}
