package org.openl.rules.ruleservice.storelogdata.db;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;

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
        supportedInjects = Set.of(new Inject<>(InjectEntityManager.class, this::getEntityManager, EntityManager::close));
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
        var injectEntityManager = (InjectEntityManager) annotation;
        Class<?>[] entityClasses;
        if (injectEntityManager.value().length == 0) {
            var storeLogDataToDB = m.getAnnotation(StoreLogDataToDB.class);
            if (storeLogDataToDB != null) {
                entityClasses = storeLogDataToDB.value();
            } else {
                entityClasses = injectEntityManager.value();
            }
        } else {
            entityClasses = injectEntityManager.value();
        }
        var entityManagerFactory = hibernateSessionOperations.getSessionFactory(entityClasses);
        return entityManagerFactory.createEntityManager();
    }

    @Override
    public Collection<Inject<?>> additionalInjects() {
        return supportedInjects;
    }

    @Override
    protected void save(StoreLogData storeLogData, boolean sync) throws StoreLogDataException {
        var storeLogDataToDBAnnotation = storeLogData.getServiceClass()
                .getAnnotation(StoreLogDataToDB.class);
        var serviceMethod = storeLogData.getServiceMethod();
        if (serviceMethod != null && serviceMethod.isAnnotationPresent(StoreLogDataToDB.class)) {
            storeLogDataToDBAnnotation = serviceMethod.getAnnotation(StoreLogDataToDB.class);
        }
        if (storeLogDataToDBAnnotation == null) {
            return;
        }
        var entities = new ArrayList<Object>();
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
                            entities.add(entityClass.getDeclaredConstructor().newInstance());
                        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException |
                                 InvocationTargetException e) {
                            throw new StoreLogDataException(
                                    "Failed to instantiate entity class '%s'%s."
                                            .formatted(
                                                    entityClass.getTypeName(),
                                                    serviceMethod != null ? (" for method '" + MethodUtil
                                                            .printQualifiedMethodName(serviceMethod) + "'") : StringUtils.EMPTY),
                                    e);
                        }
                    }
                }
            }
        }
        var entityClasses = new HashSet<Class<?>>();
        for (Object entity : entities) {
            try {
                storeLogDataMapper.map(storeLogData, entity);
                entityClasses.add(entity.getClass());
            } catch (Exception e) {
                if (serviceMethod != null) {
                    throw new StoreLogDataException("Failed to populate entity '%s' for method '%s'.".formatted(
                            entity.getClass().getTypeName(),
                            MethodUtil.printQualifiedMethodName(serviceMethod)), e);
                } else {
                    throw new StoreLogDataException(
                            "Failed to populate entity '%s'.".formatted(entity.getClass().getTypeName()),
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
