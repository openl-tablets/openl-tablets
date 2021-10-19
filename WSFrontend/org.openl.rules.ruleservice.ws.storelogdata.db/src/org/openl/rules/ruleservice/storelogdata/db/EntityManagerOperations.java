package org.openl.rules.ruleservice.storelogdata.db;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.PersistenceException;

import org.openl.rules.ruleservice.core.OpenLService;
import org.openl.rules.ruleservice.publish.RuleServicePublisherListener;
import org.openl.spring.config.ConditionalOnEnable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnEnable("ruleservice.store.logs.db.enabled")
public class EntityManagerOperations implements RuleServicePublisherListener {
    private final Logger log = LoggerFactory.getLogger(EntityManagerOperations.class);

    @Autowired
    private HibernateSessionFactoryBuilder hibernateSessionFactoryBuilder;

    private static class Key {
        final Set<Class<?>> entityClasses;

        public Key(Class<?>[] entityClasses) {
            this.entityClasses = new HashSet<>(
                Arrays.asList(Objects.requireNonNull(entityClasses, "entityClasses cannot be null")));
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

    private final AtomicReference<Map<Key, EntityManagerFactory>> entityManagers = new AtomicReference<>(
        Collections.unmodifiableMap(new HashMap<>()));

    public void save(Class<?>[] entityClasses, Object entity, boolean sync) {
        if (entity == null) {
            return;
        }
        try {
            EntityManager entityManager = getEntityManagerFactory(entityClasses).createEntityManager();
            EntityTransaction tx = entityManager.getTransaction();
            tx.begin();
            entityManager.persist(entity);
            tx.commit();
            entityManager.close();
        } catch (PersistenceException e) {
            log.error("Failed to save entity '{}'.", entity.getClass().getTypeName(), e);
        }
    }

    public EntityManagerFactory getEntityManagerFactory(Class<?>[] entityClasses) {
        EntityManagerFactory entityManagerFactory = null;
        Map<Key, EntityManagerFactory> current;
        Map<Key, EntityManagerFactory> next;
        do {
            current = entityManagers.get();
            Key key = new Key(entityClasses);
            EntityManagerFactory currentEntityManager = current.get(key);
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
        entityManagers.set(Collections.emptyMap());
    }

    public HibernateSessionFactoryBuilder getHibernateSessionFactoryBuilder() {
        return hibernateSessionFactoryBuilder;
    }

    public void setHibernateSessionFactoryBuilder(HibernateSessionFactoryBuilder hibernateSessionFactoryBuilder) {
        this.hibernateSessionFactoryBuilder = hibernateSessionFactoryBuilder;
    }
}
