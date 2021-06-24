package org.openl.rules.ruleservice.storelogdata.hive;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.apache.commons.lang3.StringUtils;
import org.openl.binding.MethodUtil;
import org.openl.rules.ruleservice.storelogdata.AbstractStoreLogDataService;
import org.openl.rules.ruleservice.storelogdata.Inject;
import org.openl.rules.ruleservice.storelogdata.StoreLogData;
import org.openl.rules.ruleservice.storelogdata.StoreLogDataException;
import org.openl.rules.ruleservice.storelogdata.StoreLogDataMapper;
import org.openl.rules.ruleservice.storelogdata.annotation.AnnotationUtils;
import org.openl.rules.ruleservice.storelogdata.hive.annotation.HiveConnection;
import org.openl.rules.ruleservice.storelogdata.hive.annotation.StoreLogDataToHive;
import org.openl.spring.config.ConditionalOnEnable;
import org.openl.util.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnEnable("ruleservice.store.logs.hive.enabled")
public class HiveStoreLogDataService extends AbstractStoreLogDataService {

    private final StoreLogDataMapper storeLogDataMapper = new StoreLogDataMapper();

    @Autowired
    private HiveOperations hiveOperations;

    private volatile Collection<Inject<?>> supportedInjects;

    public void setHiveOperations(HiveOperations hiveOperations) {
        this.hiveOperations = hiveOperations;
    }

    @Override
    public boolean isSync(StoreLogData storeLogData) {
        StoreLogDataToHive storeLogDataToHive = AnnotationUtils.getAnnotationInServiceClassOrServiceMethod(storeLogData,
            StoreLogDataToHive.class);
        if (storeLogDataToHive != null) {
            return storeLogDataToHive.sync();
        }
        return false;
    }

    @Override
    public Collection<Inject<?>> additionalInjects() {
        if (supportedInjects == null) {
            synchronized (this) {
                if (supportedInjects == null) {
                    Collection<Inject<?>> injects = new ArrayList<>();
                    injects.add(new Inject<>(HiveConnection.class,
                        () -> hiveOperations.getConnection(),
                        IOUtils::closeQuietly));
                    supportedInjects = Collections.unmodifiableCollection(injects);
                }
            }
        }
        return supportedInjects;
    }

    @Override
    protected void save(StoreLogData storeLogData, boolean sync) throws StoreLogDataException {
        StoreLogDataToHive storeLogDataAnnotation = getAnnotation(storeLogData);
        if (storeLogDataAnnotation == null)
            return;

        Object[] entities = getEntities(storeLogDataAnnotation, storeLogData.getServiceMethod());

        mapEntities(storeLogData, entities, storeLogData.getServiceMethod());
        saveEntities(entities);
    }

    private void saveEntities(Object[] entities) throws StoreLogDataException {
        for (Object entity : entities) {
            if (entity != null) {
                try {
                    hiveOperations.save(entity);
                } catch (Exception e) {
                    throw new StoreLogDataException("Failed on hive entity save operation.", e);
                }
            }
        }
    }

    private void mapEntities(StoreLogData storeLogData,
            Object[] entities,
            Method serviceMethod) throws StoreLogDataException {
        for (Object entity : entities) {
            try {
                storeLogDataMapper.map(storeLogData, entity);
            } catch (Exception e) {
                if (serviceMethod != null) {
                    throw new StoreLogDataException(
                        String.format("Failed to populate hive entity '%s' for method '%s'.",
                            entity.getClass().getTypeName(),
                            MethodUtil.printQualifiedMethodName(serviceMethod)),
                        e);
                } else {
                    throw new StoreLogDataException(
                        String.format("Failed to populate hive entity '%s'.", entity.getClass().getTypeName()),
                        e);
                }
            }
        }
    }

    private Object[] getEntities(StoreLogDataToHive storeLogDataAnnotation,
            Method serviceMethod) throws StoreLogDataException {
        Object[] entities;
        if (storeLogDataAnnotation.value().length == 0) {
            entities = new DefaultHiveEntity[] { new DefaultHiveEntity() };
        } else {
            entities = new Object[storeLogDataAnnotation.value().length];
            int i = 0;
            for (Class<?> entityClass : storeLogDataAnnotation.value()) {
                if (StoreLogDataToHive.DEFAULT.class == entityClass) {
                    entities[i] = new DefaultHiveEntity();
                } else {
                    try {
                        entities[i] = entityClass.getDeclaredConstructor().newInstance();
                    } catch (Exception e) {
                        throw new StoreLogDataException(String.format(
                            "Failed to instantiate Hive entity '%s'. Please, check that class '%s' is not abstract and has a default constructor.",
                            serviceMethod != null ? " for method '" + MethodUtil
                                .printQualifiedMethodName(serviceMethod) + "'" : StringUtils.EMPTY,
                            entityClass.getTypeName()), e);
                    }
                }
                i++;
            }
        }
        return entities;
    }

    private StoreLogDataToHive getAnnotation(StoreLogData storeLogData) {
        StoreLogDataToHive storeLogDataAnnotation = storeLogData.getServiceClass()
            .getAnnotation(StoreLogDataToHive.class);
        Method serviceMethod = storeLogData.getServiceMethod();
        if (serviceMethod != null && serviceMethod.isAnnotationPresent(StoreLogDataToHive.class)) {
            storeLogDataAnnotation = serviceMethod.getAnnotation(StoreLogDataToHive.class);
        }
        return storeLogDataAnnotation;
    }

}
