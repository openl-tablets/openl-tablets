package org.openl.rules.ruleservice.storelogdata.hive;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

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
        StoreLogDataToHive storeLogDataToHive = getAnnotation(storeLogData);
        if (storeLogDataToHive == null) {
            return;
        }
        List<Object> entities = getEntities(storeLogData, storeLogDataToHive);
        mapEntities(storeLogData, entities, storeLogData.getServiceMethod());
        saveEntities(entities);
    }

    private void saveEntities(List<Object> entities) throws StoreLogDataException {
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
            List<Object> entities,
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

    private List<Object> getEntities(StoreLogData storeLogData,
            StoreLogDataToHive storeLogDataToHive) throws StoreLogDataException {
        List<Object> entities = new ArrayList<>();
        if (storeLogDataToHive.value().length == 0) {
            if (!storeLogData.isIgnorable(DefaultHiveEntity.class)) {
                entities.add(new DefaultHiveEntity());
            }
        } else {
            for (Class<?> entityClass : storeLogDataToHive.value()) {
                if (!storeLogData.isIgnorable(entityClass)) {
                    if (StoreLogDataToHive.DEFAULT.class == entityClass) {
                        entities.add(new DefaultHiveEntity());
                    } else {
                        try {
                            entities.add(entityClass.getDeclaredConstructor().newInstance());
                        } catch (Exception e) {
                            throw new StoreLogDataException(String.format(
                                "Failed to instantiate Hive entity '%s'. Please, check that class '%s' is not abstract and has a default constructor.",
                                storeLogData.getServiceMethod() != null ? " for method '" + MethodUtil
                                    .printQualifiedMethodName(storeLogData.getServiceMethod()) + "'"
                                                                        : StringUtils.EMPTY,
                                entityClass.getTypeName()), e);
                        }
                    }
                }
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
