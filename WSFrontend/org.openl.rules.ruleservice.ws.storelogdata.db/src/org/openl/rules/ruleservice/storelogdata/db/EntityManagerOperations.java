package org.openl.rules.ruleservice.storelogdata.db;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.openl.rules.ruleservice.core.OpenLService;
import org.openl.rules.ruleservice.publish.RuleServicePublisherListener;
import org.openl.spring.config.ConditionalOnEnable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnEnable("ruleservice.store.logs.db.enabled")
public class EntityManagerOperations implements RuleServicePublisherListener {
    private final Logger log = LoggerFactory.getLogger(EntityManagerOperations.class);

    private final HibernateSessionFactoryBuilder hibernateSessionFactoryBuilder;

    public EntityManagerOperations(HibernateSessionFactoryBuilder hibernateSessionFactoryBuilder) {
        this.hibernateSessionFactoryBuilder = hibernateSessionFactoryBuilder;
    }

    private static class Key {
        final Set<Class<?>> entityClasses;

        public Key(Class<?>[] entityClasses) {
            this.entityClasses = Set.of(Objects.requireNonNull(entityClasses, "entityClasses cannot be null"));
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;

            Key key = (Key) o;

            return entityClasses.equals(key.entityClasses);
        }

        @Override
        public int hashCode() {
            return entityClasses.hashCode();
        }
    }

    private final AtomicReference<Map<Key, SessionFactory>> entityManagers = new AtomicReference<>(
        Collections.unmodifiableMap(new HashMap<>()));

    public void save(Class<?>[] entityClasses, Object entity) {
        if (entity == null) {
            return;
        }
        try (var session = getSessionFactory(entityClasses).openSession()) {
            Transaction tx = null;
            try {
                tx = session.beginTransaction();
                session.merge(entity);
                tx.commit();
            } catch (RuntimeException e) {
                if (tx != null && tx.isActive()) {
                    tx.rollback();
                }
                throw e;
            }
        }
    }

    public SessionFactory getSessionFactory(Class<?>[] entityClasses) {
        SessionFactory entityManagerFactory = null;
        Map<Key, SessionFactory> current;
        Map<Key, SessionFactory> next;
        do {
            current = entityManagers.get();
            Key key = new Key(entityClasses);
            SessionFactory currentEntityManager = current.get(key);
            if (currentEntityManager != null) {
                return currentEntityManager;
            } else {
                if (entityManagerFactory == null) {
                    entityManagerFactory = hibernateSessionFactoryBuilder.buildSessionFactory(entityClasses);
                }
                next = new HashMap<>(current);
                next.put(key, entityManagerFactory);
            }
        } while (!entityManagers.compareAndSet(current, Collections.unmodifiableMap(next)));
        return entityManagerFactory;
    }

    @Override
    public void onDeploy(OpenLService service) {
        // Only onUndeploy is used for clear used classes to prevent memory leak.
    }

    @Override
    public void onUndeploy(String deployPath) {
        var emToClose = entityManagers.get();
        entityManagers.set(Collections.emptyMap());
        for (SessionFactory sf : emToClose.values()) {
            try {
                sf.close();
            } catch (RuntimeException e) {
                log.error("Failed to close SessionFactory", e);
            }
        }
    }
}
