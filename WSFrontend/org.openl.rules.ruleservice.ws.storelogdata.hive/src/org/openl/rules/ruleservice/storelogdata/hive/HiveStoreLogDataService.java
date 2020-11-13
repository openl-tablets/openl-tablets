package org.openl.rules.ruleservice.storelogdata.hive;

import java.lang.reflect.Method;

import org.apache.commons.lang3.StringUtils;
import org.openl.binding.MethodUtil;
import org.openl.rules.ruleservice.storelogdata.StoreLogData;
import org.openl.rules.ruleservice.storelogdata.StoreLogDataMapper;
import org.openl.rules.ruleservice.storelogdata.StoreLogDataService;
import org.openl.rules.ruleservice.storelogdata.hive.annotation.StoreLogDataToHive;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HiveStoreLogDataService implements StoreLogDataService {

    private final Logger log = LoggerFactory.getLogger(HiveStoreLogDataService.class);
    private final StoreLogDataMapper storeLogDataMapper = new StoreLogDataMapper();
    private HiveOperations hiveOperations;
    private boolean enabled = true;

    public void setHiveOperations(HiveOperations hiveOperations) {
        this.hiveOperations = hiveOperations;
    }

    @Override
    public void save(StoreLogData storeLogData) {
        StoreLogDataToHive storeLogDataAnnotation = getAnnotation(storeLogData);
        if (storeLogDataAnnotation == null)
            return;

        Object[] entities = getEntities(storeLogDataAnnotation, storeLogData.getServiceMethod());
        if (entities == null)
            return;

        mapEntities(storeLogData, entities, storeLogData.getServiceMethod());
        saveEntities(entities);
    }

    private void saveEntities(Object[] entities) {
        for (Object entity : entities) {
            if (entity != null) {
                try {
                    hiveOperations.save(entity);
                } catch (Exception e) {
                    log.error("Failed on hive entity save operation.", e);
                }
            }
        }
    }

    private void mapEntities(StoreLogData storeLogData, Object[] entities, Method serviceMethod) {
        for (Object entity : entities) {
            try {
                storeLogDataMapper.map(storeLogData, entity);
            } catch (Exception e) {
                if (log.isErrorEnabled()) {
                    if (serviceMethod != null) {
                        log.error("Failed to populate hive entity '{}' for method '{}'.",
                            entity.getClass().getTypeName(),
                            MethodUtil.printQualifiedMethodName(serviceMethod),
                            e);
                    } else {
                        log.error("Failed to populate hive entity '{}'.", entity.getClass().getTypeName(), e);
                    }
                }
                return;
            }
        }
    }

    private Object[] getEntities(StoreLogDataToHive storeLogDataAnnotation, Method serviceMethod) {
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
                        if (log.isErrorEnabled()) {
                            log.error(String.format(
                                "Failed to instantiate Hive entity '%s'. Please, check that class '%s' is not abstract and has a default constructor.",
                                serviceMethod != null ? " for method '" + MethodUtil
                                    .printQualifiedMethodName(serviceMethod) + "'" : StringUtils.EMPTY,
                                entityClass.getTypeName()), e);
                        }
                        return null;
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

    @Override
    public boolean isEnabled() {
        return this.enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
