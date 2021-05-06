package org.openl.rules.ruleservice.storelogdata;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.openl.rules.ruleservice.storelogdata.annotation.SkipFault;
import org.openl.rules.ruleservice.storelogdata.annotation.SyncSave;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SimpleStoreLogDataManager implements StoreLogDataManager {
    private final Logger log = LoggerFactory.getLogger(SimpleStoreLogDataManager.class);

    private final Collection<StoreLogDataService> storeLogDataServices;

    public SimpleStoreLogDataManager(Collection<StoreLogDataService> storeLogDataServices) {
        Objects.requireNonNull(storeLogDataServices);
        this.storeLogDataServices = new ArrayList<>(storeLogDataServices);
    }

    ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Override
    public void store(StoreLogData storeLogData) {
        if (!storeLogData.isIgnorable()) {
            if (!ignoreByFault(storeLogData)) {
                if (isSyncSave(storeLogData)) {
                    save(storeLogData);
                } else {
                    executorService.submit(() -> {
                        save(storeLogData);
                    });
                }
            }
        }
    }

    private void save(StoreLogData storeLogData) {
        for (StoreLogDataService storeLoggingService : storeLogDataServices) {
            try {
                storeLoggingService.save(storeLogData);
            } catch (Exception e) {
                log.error("Failed on save operation.", e);
            }
        }
    }

    private static boolean isAnnotationPresentInServiceClassOrServiceMethod(StoreLogData storeLogData,
            Class<? extends Annotation> annotationClass) {
        if (storeLogData.getServiceClass() != null && storeLogData.getServiceClass()
            .isAnnotationPresent(annotationClass)) {
            return true;
        }
        return storeLogData.getServiceMethod() != null && storeLogData.getServiceMethod()
            .isAnnotationPresent(annotationClass);
    }

    private boolean isSyncSave(StoreLogData storeLogData) {
        return isAnnotationPresentInServiceClassOrServiceMethod(storeLogData, SyncSave.class);
    }

    private boolean ignoreByFault(StoreLogData storeLogData) {
        if (storeLogData.isFault()) {
            return isAnnotationPresentInServiceClassOrServiceMethod(storeLogData, SkipFault.class);
        }
        return false;
    }
}
