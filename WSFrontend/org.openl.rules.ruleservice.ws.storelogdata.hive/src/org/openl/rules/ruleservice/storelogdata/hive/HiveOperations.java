package org.openl.rules.ruleservice.storelogdata.hive;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.openl.rules.ruleservice.core.OpenLService;
import org.openl.rules.ruleservice.publish.RuleServicePublisherListener;
import org.openl.rules.ruleservice.storelogdata.hive.annotation.Entity;
import org.openl.util.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HiveOperations implements RuleServicePublisherListener {
    private final Logger log = LoggerFactory.getLogger(HiveOperations.class);

    private boolean createTableEnabled = false;
    private final AtomicReference<Set<Class<?>>> entitiesWithAlreadyCreatedSchema = new AtomicReference<>(
            Collections.unmodifiableSet(new HashSet<>()));
    private final AtomicReference<Map<Class<?>, HiveEntityDao>> entitySavers = new AtomicReference<>(
        Collections.unmodifiableMap(new HashMap<>()));

    @Override
    public void afterPropertiesSet() {
        try {
            init();
        } catch (Exception e) {
            log.error("Hive initialization failure.", e);
        }
    }

    public Connection getConnection() {
        return HiveDataSource.getConnection();
    }

    @Override
    public void onDeploy(OpenLService service) {
        // Only onUndeploy is used for clear used classes to prevent memory leak.
    }

    @Override
    public void onUndeploy(String deployPath) {
        entitiesWithAlreadyCreatedSchema.set(Collections.emptySet());
        entitySavers.set(Collections.emptyMap());
    }

    public void save(Object entity) {
        if (entity == null) {
            return;
        }
        try (Connection connection = getConnection()) {
            createTableIfNotExists(connection, entity.getClass());
            getHiveEntityDao(entity.getClass()).insert(connection, entity);
        } catch (Exception e) {
            log.error("Failed to save hive entity.", e);
        }
    }

    private HiveEntityDao getHiveEntityDao(Class<?> entityClass) throws UnsupportedFieldTypeException {
        HiveEntityDao hiveEntityDao = null;
        Map<Class<?>, HiveEntityDao> current;
        Map<Class<?>, HiveEntityDao> next;
        do {
            current = entitySavers.get();
            HiveEntityDao currentEntitySaver = current.get(entityClass);
            if (currentEntitySaver != null) {
                return currentEntitySaver;
            } else {
                if (hiveEntityDao == null) {
                    hiveEntityDao = new HiveEntityDao(entityClass);
                }
                next = new HashMap<>(current);
                next.put(entityClass, hiveEntityDao);
            }
        } while (!entitySavers.compareAndSet(current, Collections.unmodifiableMap(next)));
        return hiveEntityDao;
    }

    public void createTableIfNotExists(Connection connection, Class<?> entityClass) {
        if (isCreateTableEnabled()) {
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
                    String sqlQuery = extractSqlQueryForEntity(entityClass);
                    String[] queries = sqlQuery.split(";");
                    for (String q : queries) {
                        try (Statement statement = connection.createStatement()) {
                            statement.execute(q.trim());
                        }
                    }
                } catch (IOException | SQLException e) {
                    throw new HiveTableCreationException(
                            String.format("Failed to extract a file with schema creation SQL for '%s'.",
                                    entityClass.getTypeName()),
                            e);
                }
            } else {
                throw new HiveTableCreationException(
                        String.format("Missed @Entity annotation for hive entity class '%s'.", entityClass.getTypeName()));
            }
        }
    }

    public boolean isCreateTableEnabled() {
        return createTableEnabled;
    }

    public void setCreateTableEnabled(boolean createTableEnabled) {
        this.createTableEnabled = createTableEnabled;
    }

    public void setConnectionURL(String connectionURL) {
        this.connectionURL = connectionURL;
    }

    private static String extractSqlQueryForEntity(Class<?> entityClass) throws IOException {
        InputStream inputStream = entityClass
                .getResourceAsStream("/" + entityClass.getName().replaceAll("\\.", "/") + ".sql");
        if (inputStream == null) {
            throw new FileNotFoundException("/" + entityClass.getName().replaceAll("\\.", "/") + ".sql");
        }
        return IOUtils.toStringAndClose(inputStream);
    }
}
