package org.openl.rules.ruleservice.storelogdata.db;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.openl.binding.MethodUtil;
import org.openl.rules.ruleservice.storelogdata.AbstractStoreLogDataService;
import org.openl.rules.ruleservice.storelogdata.Inject;
import org.openl.rules.ruleservice.storelogdata.StoreLogData;
import org.openl.rules.ruleservice.storelogdata.StoreLogDataException;
import org.openl.rules.ruleservice.storelogdata.StoreLogDataMapper;
import org.openl.rules.ruleservice.storelogdata.annotation.AnnotationUtils;
import org.openl.rules.ruleservice.storelogdata.db.annotation.InjectEntityManager;
import org.openl.rules.ruleservice.storelogdata.db.annotation.StoreLogDataToDB;
import org.openl.spring.config.ConditionalOnEnable;

@Component
@ConditionalOnEnable("ruleservice.store.logs.db.enabled")
public class DBStoreLogDataService extends AbstractStoreLogDataService {

    @Autowired
    private EntityManagerOperations hibernateSessionOperations;

    private final StoreLogDataMapper storeLogDataMapper = new StoreLogDataMapper();

    private Collection<Inject<?>> supportedInjects;

    @PostConstruct
    public void setup() {
        supportedInjects = Collections
                .singleton(new Inject<>(InjectEntityManager.class, this::getEntityManager, EntityManager::close));
    }

    @Override
    public boolean isSync(StoreLogData storeLogData) {
        StoreLogDataToDB storeLogDataToDB = AnnotationUtils.getAnnotationInServiceClassOrServiceMethod(storeLogData,
                StoreLogDataToDB.class);
        if (storeLogDataToDB != null) {
            return storeLogDataToDB.sync();
        }
        return false;
    }

    private EntityManager getEntityManager(Method m, Annotation annotation) {
        InjectEntityManager injectEntityManager = (InjectEntityManager) annotation;
        Class<?>[] entityClasses;
        if (injectEntityManager.value().length == 0) {
            StoreLogDataToDB storeLogDataToDB = m.getAnnotation(StoreLogDataToDB.class);
            if (storeLogDataToDB != null) {
                entityClasses = storeLogDataToDB.value();
            } else {
                entityClasses = injectEntityManager.value();
            }
        } else {
            entityClasses = injectEntityManager.value();
        }
        SessionFactory entityManagerFactory = hibernateSessionOperations.getSessionFactory(entityClasses);
        return entityManagerFactory.createEntityManager();
    }

    @Override
    public Collection<Inject<?>> additionalInjects() {
        return supportedInjects;
    }

    @Override
    protected void save(StoreLogData storeLogData, boolean sync) throws StoreLogDataException {
        StoreLogDataToDB storeLogDataToDBAnnotation = storeLogData.getServiceClass()
                .getAnnotation(StoreLogDataToDB.class);
        Method serviceMethod = storeLogData.getServiceMethod();
        if (serviceMethod != null && serviceMethod.isAnnotationPresent(StoreLogDataToDB.class)) {
            storeLogDataToDBAnnotation = serviceMethod.getAnnotation(StoreLogDataToDB.class);
        }
        if (storeLogDataToDBAnnotation == null) {
            return;
        }
        List<Object> entities = new ArrayList<>();
        if (storeLogDataToDBAnnotation.value().length == 0) {
            if (!storeLogData.isIgnorable(DefaultEntity.class)) {
                entities.add(new DefaultEntity());
            }
        } else {
            for (Class<?> entityClass : storeLogDataToDBAnnotation.value()) {
                if (!storeLogData.isIgnorable(entityClass)) {
                    if (StoreLogDataToDB.DEFAULT.class == entityClass) {
                        entities.add(new DefaultEntity());
                    } else {
                        try {
                            entities.add(entityClass.newInstance());
                        } catch (InstantiationException | IllegalAccessException e) {
                            throw new StoreLogDataException(
                                    String
                                            .format("Failed to instantiate entity class '%s'%s.",
                                                    entityClass.getTypeName(),
                                                    serviceMethod != null ? (" for method '" + MethodUtil
                                                            .printQualifiedMethodName(serviceMethod) + "'") : StringUtils.EMPTY),
                                    e);
                        }
                    }
                }
            }
        }
        Set<Class<?>> entityClasses = new HashSet<>();
        for (Object entity : entities) {
            try {
                storeLogDataMapper.map(storeLogData, entity);
                entityClasses.add(entity.getClass());
            } catch (Exception e) {
                if (serviceMethod != null) {
                    throw new StoreLogDataException(String.format("Failed to populate entity '%s' for method '%s'.",
                            entity.getClass().getTypeName(),
                            MethodUtil.printQualifiedMethodName(serviceMethod)), e);
                } else {
                    throw new StoreLogDataException(
                            String.format("Failed to populate entity '%s'.", entity.getClass().getTypeName()),
                            e);
                }
            }
        }
        for (Object entity : entities) {
            if (entity != null) {
                try {
                    hibernateSessionOperations.save(entityClasses.toArray(new Class<?>[0]), entity);
                } catch (Exception e) {
                    // Continue the loop if exception occurs
                    throw new StoreLogDataException("Failed on database save operation.", e);
                }
            }
        }
    }
}
