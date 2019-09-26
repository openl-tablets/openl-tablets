package org.openl.rules.ruleservice.logging;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.openl.rules.ruleservice.logging.annotation.IgnoreStoreLogging;
import org.openl.rules.ruleservice.logging.annotation.OnlySuccessStoreLogging;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SimpleStoreLoggingManager implements StoreLoggingManager {
    private final Logger log = LoggerFactory.getLogger(SimpleStoreLoggingManager.class);

    private Collection<StoreLoggingService> storeLoggingServices;

    public SimpleStoreLoggingManager(Collection<StoreLoggingService> storeLoggingServices) {
        Objects.requireNonNull(storeLoggingServices);
        this.storeLoggingServices = new ArrayList<>(storeLoggingServices);
    }

    ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Override
    public void store(StoreLoggingData storeLoggingData) {
        if (!storeLoggingData.isIgnorable()) {
            Method serviceMethod = storeLoggingData.getServiceMethod();
            if (serviceMethod == null || !serviceMethod.isAnnotationPresent(IgnoreStoreLogging.class)) {
                if (!ignoreByFault(storeLoggingData, serviceMethod)) {
                    executorService.submit(() -> {
                        for (StoreLoggingService storeLoggingService : storeLoggingServices) {
                            try {
                                storeLoggingService.save(storeLoggingData);
                            } catch (Exception e) {
                                log.error("Failed on save operation.", e);
                            }
                        }
                    });
                }
            }
        }
    }

    private boolean ignoreByFault(StoreLoggingData storeLoggingData, Method serviceMethod) {
        return storeLoggingData.isFault() && serviceMethod != null && serviceMethod
            .isAnnotationPresent(OnlySuccessStoreLogging.class);
    }
}
