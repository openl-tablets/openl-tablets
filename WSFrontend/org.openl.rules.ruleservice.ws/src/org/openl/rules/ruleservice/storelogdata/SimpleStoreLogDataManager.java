package org.openl.rules.ruleservice.storelogdata;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.openl.rules.ruleservice.storelogdata.annotation.SkipFaultStoreLogData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SimpleStoreLogDataManager implements StoreLogDataManager {
    private final Logger log = LoggerFactory.getLogger(SimpleStoreLogDataManager.class);

    private Collection<StoreLogDataService> storeLogDataServices;

    public SimpleStoreLogDataManager(Collection<StoreLogDataService> storeLogDataServices) {
        Objects.requireNonNull(storeLogDataServices);
        this.storeLogDataServices = new ArrayList<>(storeLogDataServices);
    }

    ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Override
    public void store(StoreLogData storeLogData) {
        if (!storeLogData.isIgnorable()) {
            if (!ignoreByFault(storeLogData)) {
                executorService.submit(() -> {
                    for (StoreLogDataService storeLoggingService : storeLogDataServices) {
                        try {
                            storeLoggingService.save(storeLogData);
                        } catch (Exception e) {
                            log.error("Failed on save operation.", e);
                        }
                    }
                });
            }
        }
    }

    private boolean ignoreByFault(StoreLogData storeLogData) {
        Method serviceMethod = storeLogData.getServiceMethod();
        return storeLogData.isFault() && serviceMethod != null && serviceMethod
            .isAnnotationPresent(SkipFaultStoreLogData.class);
    }
}
