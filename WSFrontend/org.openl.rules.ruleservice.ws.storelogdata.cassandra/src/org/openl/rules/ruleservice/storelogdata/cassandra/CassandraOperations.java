package org.openl.rules.ruleservice.storelogdata.cassandra;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.openl.rules.ruleservice.core.OpenLService;
import org.openl.rules.ruleservice.publish.RuleServicePublisherListener;
import org.openl.rules.ruleservice.storelogdata.cassandra.annotation.DaoCreationException;
import org.openl.rules.ruleservice.storelogdata.cassandra.annotation.EntityOperations;
import org.openl.rules.ruleservice.storelogdata.cassandra.annotation.EntitySupport;
import org.openl.util.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.DriverException;
import com.datastax.oss.driver.api.core.session.Session;
import com.datastax.oss.driver.api.core.type.codec.TypeCodecs;
import com.datastax.oss.driver.api.mapper.annotations.Entity;

public class CassandraOperations implements InitializingBean, DisposableBean, RuleServicePublisherListener, ApplicationContextAware {
    private final Logger log = LoggerFactory.getLogger(CassandraOperations.class);

    private CqlSession session;
    private boolean schemaCreationEnabled = false;
    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    private synchronized void init() {
        if (session == null) {
            session = CqlSession.builder()
                .withConfigLoader(ConfigLoader.fromApplicationContext(applicationContext))
                .addTypeCodecs(TypeCodecs.ZONED_TIMESTAMP_SYSTEM)
                .build();
        } else {
            throw new IllegalStateException("Session is already initialized!");
        }
    }

    public Session connect() {
        if (session == null) {
            init();
        }
        return session;
    }

    @Override
    public void destroy() {
        if (session != null) {
            try {
                session.close();
            } catch (Exception e) {
                log.error("Failed to close Cassandra connection.", e);
            }
        }
    }

    private AtomicReference<Set<Class<?>>> entitiesWithAlreadyCreatedSchema = new AtomicReference<>(
        Collections.unmodifiableSet(new HashSet<>()));
    private AtomicReference<Map<Class<?>, CassandraEntitySaver>> entitySavers = new AtomicReference<>(
        Collections.unmodifiableMap(new HashMap<>()));

    protected CassandraEntitySaver getEntitySaver(Class<?> entityClass) throws DaoCreationException,
                                                                        ReflectiveOperationException {
        CassandraEntitySaver cassandraEntitySaver = null;
        Map<Class<?>, CassandraEntitySaver> current;
        Map<Class<?>, CassandraEntitySaver> next;
        do {
            current = entitySavers.get();
            CassandraEntitySaver currentCassandraEntitySaver = current.get(entityClass);
            if (currentCassandraEntitySaver != null) {
                return currentCassandraEntitySaver;
            } else {
                if (cassandraEntitySaver == null) {
                    cassandraEntitySaver = createCassandraEntitySaver(entityClass);
                }
                next = new HashMap<>(current);
                next.put(entityClass, cassandraEntitySaver);
            }
        } while (!entitySavers.compareAndSet(current, Collections.unmodifiableMap(next)));
        return cassandraEntitySaver;
    }

    private CassandraEntitySaver createCassandraEntitySaver(
            Class<?> entityClass) throws InstantiationException, IllegalAccessException, DaoCreationException {
        EntitySupport entitySupport = entityClass.getAnnotation(EntitySupport.class);
        if (entitySupport == null) {
            throw new DaoCreationException(String.format(
                "Failed to save cassandra entity. Annotation @EntitySupport is not presented in class '%s'.",
                entityClass.getTypeName()));
        } else {
            Class<? extends EntityOperations<?, ?>> entityOperationsClass = entitySupport.value();
            @SuppressWarnings("unchecked")
            EntityOperations<Object, Object> entityOperations = (EntityOperations<Object, Object>) entityOperationsClass
                .newInstance();
            Object dao = entityOperations.buildDao(session);
            return new CassandraEntitySaver(entityOperations, dao);
        }
    }

    public void save(Object entity) {
        if (entity == null) {
            return;
        }
        try {
            createSchemaIfMissed(entity.getClass());
            EntitySupport entitySupport = entity.getClass().getAnnotation(EntitySupport.class);
            if (entitySupport == null) {
                log.error("Failed to save cassandra entity. Annotation @EntitySupport is not presented in class {}.",
                    entity.getClass().getTypeName());
            } else {
                getEntitySaver(entity.getClass()).insert(entity);
            }
        } catch (ReflectiveOperationException | DaoCreationException e) {
            log.error("Failed to save cassandra entity.", e);
        }
    }

    @Override
    public void onDeploy(OpenLService service) {
        // Only onUndeploy is used for clear used classes to prevent memory leak.
    }

    @Override
    public void onUndeploy(String serviceName) {
        entitiesWithAlreadyCreatedSchema.set(Collections.emptySet());
        entitySavers.set(Collections.emptyMap());
    }

    public void createSchemaIfMissed(Class<?> entityClass) {
        if (isCreateSchemaEnabled()) {
            Set<Class<?>> current;
            Set<Class<?>> next;
            do {
                current = entitiesWithAlreadyCreatedSchema.get();
                if (current.contains(entityClass)) {
                    return;
                }
                next = new HashSet<>(current);
                next.add(entityClass);
            } while (!entitiesWithAlreadyCreatedSchema.compareAndSet(current, Collections.unmodifiableSet(next)));

            Entity entity = entityClass.getAnnotation(Entity.class);
            if (entity != null) {
                try {
                    String cqlQuery = extractCqlQueryForEntity(entityClass);
                    String[] queries = cqlQuery.split(";");
                    for (String q : queries) {
                        session.execute(removeCommentsInStatement(q.trim()));
                    }
                } catch (IOException e) {
                    throw new SchemaCreationException(
                        String.format("Failed to extract a file with schema creation CQL for '%s'.",
                            entityClass.getTypeName()),
                        e);
                } catch (DriverException e) {
                    throw new SchemaCreationException(
                        String.format("Failed to execute schema creation CQL for '%s'", entityClass.getTypeName()),
                        e);
                }
            } else {
                throw new SchemaCreationException(String
                    .format("Missed @Entity annotation for cassandra entity class '%s'.", entityClass.getTypeName()));
            }
        }
    }

    protected static String removeCommentsInStatement(String statement) {
        return statement.replaceAll("(?:/\\*(?:[^*]|(?:\\*+[^*/]))*\\*+/)|(?://.*)|(?:--.*)", "");
    }

    private String extractCqlQueryForEntity(Class<?> entityClass) throws IOException {
        InputStream inputStream = entityClass
            .getResourceAsStream("/" + entityClass.getName().replaceAll("\\.", "/") + ".cql");
        if (inputStream == null) {
            throw new FileNotFoundException("/" + entityClass.getName().replaceAll("\\.", "/") + ".cql");
        }
        return IOUtils.toStringAndClose(inputStream);
    }

    public boolean isCreateSchemaEnabled() {
        return schemaCreationEnabled;
    }

    public void setCreateSchemaEnabled(boolean createSchemaEnabled) {
        this.schemaCreationEnabled = createSchemaEnabled;
    }

    @Override
    public void afterPropertiesSet() {
        try {
            init();
        } catch (Exception e) {
            log.error("Cassandra initialization failure.", e);
        }
    }
}
