package org.openl.rules.ruleservice.storelogdata;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.openl.rules.ruleservice.storelogdata.annotation.SkipFaultStoreLogData;
import org.openl.spring.config.ConditionalOnEnable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

@ConditionalOnEnable("ruleservice.store.logs.enabled")
public final class SimpleStoreLogDataManager implements StoreLogDataManager {
    private final Logger log = LoggerFactory.getLogger(SimpleStoreLogDataManager.class);

    private final Collection<StoreLogDataService> storeLogDataServices;

    @Autowired
    public SimpleStoreLogDataManager(Collection<StoreLogDataService> storeLogDataServices) {
        Objects.requireNonNull(storeLogDataServices);
        for (StoreLogDataService storeLoggingService : storeLogDataServices) {
            log.info("Store log data service '{}' is used.", storeLoggingService.getClass().getTypeName());
        }
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
